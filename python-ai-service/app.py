import sys

if sys.version_info < (3, 9):
    import backports.zoneinfo as _backports_zoneinfo
    sys.modules["zoneinfo"] = _backports_zoneinfo

from fastapi import FastAPI, UploadFile, File
from fastapi.responses import JSONResponse
from paddleocr import PaddleOCR
from pathlib import Path
import tempfile
import os
import re
import time
from decimal import Decimal, InvalidOperation
from typing import List, Dict, Optional
import json
from openai import OpenAI

DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY", "").strip()
DEEPSEEK_MODEL = os.getenv("DEEPSEEK_MODEL", "deepseek-chat").strip()

client = OpenAI(
    api_key=DEEPSEEK_API_KEY,
    base_url="https://api.deepseek.com"
) if DEEPSEEK_API_KEY else None


app = FastAPI()

@app.get("/health")
async def health():
    return {"success": True, "message": "python-ai-service is running"}

# 中文场景先用中文 OCR
ocr = PaddleOCR(use_doc_orientation_classify=False, use_doc_unwarping=False, use_textline_orientation=False, lang="ch")

SUPPORTED_IMAGE_SUFFIXES = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}

SUPPLIER_LABELS = [
    "供应商",
    "供应商名称",
    "供货单位",
    "单位名称",
    "客户名称"
]


def normalize_text(raw_text: str) -> str:
    if not raw_text:
        return ""

    text = raw_text.replace("\r", "\n")
    text = text.replace("：", ":")
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n{2,}", "\n", text)

    normalized_lines = []
    for line in text.splitlines():
        line = line.strip()
        if not line:
            continue

        # 去掉常见 OCR 噪声空格
        line = re.sub(r"\s*:\s*", ": ", line)
        line = re.sub(r"\s+", " ", line)

        normalized_lines.append(line)

    return "\n".join(normalized_lines)

def safe_decimal(value: Optional[str]) -> Optional[Decimal]:
    if value is None:
        return None
    try:
        return Decimal(str(value).strip())
    except (InvalidOperation, ValueError):
        return None

def parse_supplier_name(raw_text: str) -> str:
    lines = [line.strip() for line in raw_text.splitlines() if line.strip()]

    supplier_patterns = [
        r"供应商\s*:\s*(.+)",
        r"供应商名称\s*:\s*(.+)",
        r"供货单位\s*:\s*(.+)",
        r"单位名称\s*:\s*(.+)",
        r"客户名称\s*:\s*(.+)",
        r"供应.\s*:\s*(.+)",   # 兼容“供应肉”“供应高”这类 OCR 误识别
    ]

    for line in lines:
        for pattern in supplier_patterns:
            match = re.search(pattern, line)
            if match:
                value = match.group(1).strip()
                if value:
                    return normalize_ocr_value(value)

    return "未识别供应商"

def parse_customer_name(raw_text: str) -> str:
    lines = [line.strip() for line in raw_text.splitlines() if line.strip()]

    customer_patterns = [
        r"客户\s*:\s*(.+)",
        r"客户名称\s*:\s*(.+)",
        r"购货单位\s*:\s*(.+)",
        r"购买单位\s*:\s*(.+)",
        r"购货方\s*:\s*(.+)",
        r"单位名称\s*:\s*(.+)",
    ]

    for line in lines:
        for pattern in customer_patterns:
            match = re.search(pattern, line)
            if match:
                value = match.group(1).strip()
                if value:
                    return normalize_ocr_value(value)

    return "未识别客户"

def split_quantity_and_unit(text: str):
    if not text:
        return None, None

    text = text.strip()
    match = re.match(r"(?P<quantity>\d+(?:\.\d+)?)\s*(?P<unit>[^\d\s]+)?$", text)
    if not match:
        return None, None

    quantity = match.group("quantity")
    unit = match.group("unit") or ""
    return quantity, unit.strip()

def build_item(line_no: int,
               product_name: str,
               specification: str,
               unit: str,
               quantity_text: str,
               unit_price_text: str,
               amount_text: Optional[str] = None) -> Optional[Dict]:
    quantity_dec = safe_decimal(quantity_text)
    unit_price_dec = safe_decimal(unit_price_text)
    amount_dec = safe_decimal(amount_text) if amount_text else None

    if quantity_dec is None or unit_price_dec is None:
        return None

    if amount_dec is None:
        amount_dec = (quantity_dec * unit_price_dec).quantize(Decimal("0.01"))
    else:
        amount_dec = amount_dec.quantize(Decimal("0.01"))

    if quantity_dec == quantity_dec.to_integral_value():
        quantity_value = int(quantity_dec)
    else:
        quantity_value = float(quantity_dec)

    return {
        "lineNo": line_no,
        "productName": normalize_ocr_value((product_name or "").strip()),
        "specification": normalize_ocr_value((specification or "").strip()),
        "unit": normalize_ocr_value((unit or "").strip()),
        "quantity": quantity_value,
        "unitPrice": float(unit_price_dec),
        "amount": float(amount_dec)
    }

def parse_item_line(line: str, line_no: int) -> Optional[Dict]:
    line = line.strip()
    if not line:
        return None

    # 去掉行号前缀：1. / 1、 / 1
    line = re.sub(r"^\d+\s*[\.、]?\s*", "", line)

    # 规则1：名称 规格 50根 单价22.00 [金额1100.00]
    pattern1 = re.compile(
        r"^(?P<productName>\S+)\s+"
        r"(?P<specification>\S+)\s+"
        r"(?P<qtyUnit>\d+(?:\.\d+)?\s*[^\d\s]+)\s+"
        r"(?:单价\s*)?(?P<unitPrice>\d+(?:\.\d+)?)"
        r"(?:\s+(?:金额\s*)?(?P<amount>\d+(?:\.\d+)?))?$"
    )

    # 规则2：名称 规格 50 根 22.00 [1100.00]
    pattern2 = re.compile(
        r"^(?P<productName>\S+)\s+"
        r"(?P<specification>\S+)\s+"
        r"(?P<quantity>\d+(?:\.\d+)?)\s+"
        r"(?P<unit>[^\d\s]+)\s+"
        r"(?:单价\s*)?(?P<unitPrice>\d+(?:\.\d+)?)"
        r"(?:\s+(?:金额\s*)?(?P<amount>\d+(?:\.\d+)?))?$"
    )

    # 规则3：名称 规格 数量50 单位根 单价22.00 [金额1100.00]
    pattern3 = re.compile(
        r"^(?P<productName>\S+)\s+"
        r"(?P<specification>\S+)\s+"
        r"(?:数量\s*)?(?P<quantity>\d+(?:\.\d+)?)\s+"
        r"(?:单位\s*)?(?P<unit>[^\d\s]+)\s+"
        r"(?:单价\s*)?(?P<unitPrice>\d+(?:\.\d+)?)"
        r"(?:\s+(?:金额\s*)?(?P<amount>\d+(?:\.\d+)?))?$"
    )

    m1 = pattern1.match(line)
    if m1:
        quantity_text, unit = split_quantity_and_unit(m1.group("qtyUnit"))
        return build_item(
            line_no=line_no,
            product_name=m1.group("productName"),
            specification=m1.group("specification"),
            unit=unit,
            quantity_text=quantity_text,
            unit_price_text=m1.group("unitPrice"),
            amount_text=m1.group("amount")
        )

    m2 = pattern2.match(line)
    if m2:
        return build_item(
            line_no=line_no,
            product_name=m2.group("productName"),
            specification=m2.group("specification"),
            unit=m2.group("unit"),
            quantity_text=m2.group("quantity"),
            unit_price_text=m2.group("unitPrice"),
            amount_text=m2.group("amount")
        )

    m3 = pattern3.match(line)
    if m3:
        return build_item(
            line_no=line_no,
            product_name=m3.group("productName"),
            specification=m3.group("specification"),
            unit=m3.group("unit"),
            quantity_text=m3.group("quantity"),
            unit_price_text=m3.group("unitPrice"),
            amount_text=m3.group("amount")
        )

    return None

def parse_items_from_tokens(raw_text: str):
    lines = [line.strip() for line in raw_text.splitlines() if line.strip()]

    # 去掉明显无关内容
    filtered = []
    skip_keywords = [
        "入库单", "订单号", "仓库核对", "收货人", "合计",
        "序号", "品名称", "商品名称", "规格", "数量", "单位", "单价", "金额",
        "尿号", "奶恪", "我", "金"
    ]

    for line in lines:
        if any(keyword in line for keyword in skip_keywords):
            continue
        filtered.append(line)

    items = []
    i = 0
    line_no = 1

    def is_number_token(s: str) -> bool:
        return bool(re.fullmatch(r"\d+(?:\.\d+)?", s))

    def is_integer_token(s: str) -> bool:
        return bool(re.fullmatch(r"\d+", s))

    while i < len(filtered):
        # 找序号，如 1 / 2 / 3
        if not is_integer_token(filtered[i]):
            i += 1
            continue

        seq = filtered[i]
        # 序号后至少要有商品名
        if i + 1 >= len(filtered):
            break

        product_name = filtered[i + 1]
        i += 2

        specification = ""
        quantity = None
        unit = ""
        unit_price = None
        amount = None

        # 规格可能存在，也可能不存在
        if i < len(filtered) and not is_number_token(filtered[i]):
            # 如果下一个不是数字，也不是明显单位，优先当规格
            if filtered[i] not in ["个", "根", "台", "箱", "件", "米", "只", "包"]:
                specification = filtered[i]
                i += 1

        # 数量
        if i < len(filtered) and is_number_token(filtered[i]):
            quantity = filtered[i]
            i += 1

        # 单位
        if i < len(filtered) and not is_number_token(filtered[i]):
            unit = filtered[i]
            i += 1

        # 单价
        if i < len(filtered) and is_number_token(filtered[i]):
            unit_price = filtered[i]
            i += 1

        # 金额
        if i < len(filtered) and is_number_token(filtered[i]):
            amount = filtered[i]
            i += 1

        # 基本兜底
        if product_name and quantity and unit_price:
            item = build_item(
                line_no=line_no,
                product_name=product_name,
                specification=specification,
                unit=unit,
                quantity_text=quantity,
                unit_price_text=unit_price,
                amount_text=amount
            )
            if item:
                items.append(item)
                line_no += 1

    return items

def parse_items(raw_text: str):
    items = []
    lines = [line.strip() for line in raw_text.splitlines() if line.strip()]

    skip_keywords = [
        "供应商",
        "供应商名称",
        "供货单位",
        "单位名称",
        "客户名称"
    ]

    line_no = 1
    for line in lines:
        if any(keyword in line for keyword in skip_keywords):
            continue

        item = parse_item_line(line, line_no)
        if not item:
            continue

        items.append(item)
        line_no += 1

    if items:
        return items

    # 如果按单行规则解析不到，再尝试表格 token 模式
    return parse_items_from_tokens(raw_text)

def build_warnings(items: List[Dict]) -> List[str]:
    warnings = []

    if not items:
        warnings.append("已提取原始文本，但未成功结构化出商品明细，请人工检查")
        return warnings

    for item in items:
        quantity = item.get("quantity")
        unit_price = item.get("unitPrice")
        amount = item.get("amount")

        if quantity is None or quantity <= 0:
            warnings.append(f"第{item.get('lineNo')}行数量异常，请人工检查")

        if unit_price is None or unit_price < 0:
            warnings.append(f"第{item.get('lineNo')}行单价异常，请人工检查")

        if quantity is not None and unit_price is not None and amount is not None:
            expected_amount = round(float(quantity) * float(unit_price), 2)
            if abs(expected_amount - float(amount)) > 0.05:
                warnings.append(f"第{item.get('lineNo')}行金额与数量×单价不一致，请人工检查")

    # 去重
    return list(dict.fromkeys(warnings))

def normalize_ocr_value(text: str) -> str:
    if not text:
        return text

    replacements = {
        "供应肉": "供应商",
        "三遇换头": "三通接头",
        "俄": "根",
    }

    return replacements.get(text, text)

def ai_language_polish_result(raw_text: str,
                              supplier_name: str,
                              items: List[Dict],
                              warnings: List[str]) -> Dict:
    polish_start = time.perf_counter()
    if not client:
        return {
            "supplierName": supplier_name,
            "rawText": raw_text,
            "warnings": warnings,
            "items": items,
            "languagePolishSource": "no_llm_client",
            "languagePolishDurationMs": int((time.perf_counter() - polish_start) * 1000)
        }

    prompt = f"""
You are a Chinese OCR post-processor for warehouse inbound documents.
Return JSON only.

Tasks:
1) Polish language quality for supplierName, rawText, warnings, item.productName/specification/unit.
2) Keep business meaning unchanged.
3) DO NOT change numeric fields: lineNo, quantity, unitPrice, amount.
4) Keep array length and item order unchanged.

Input JSON:
{json.dumps({
    "supplierName": supplier_name,
    "rawText": raw_text,
    "warnings": warnings,
    "items": items
}, ensure_ascii=False)}
"""

    try:
        response = client.chat.completions.create(
            model=DEEPSEEK_MODEL,
            messages=[
                {"role": "system", "content": "Return valid JSON only."},
                {"role": "user", "content": prompt}
            ],
            response_format={"type": "json_object"}
        )
        parsed = json.loads(response.choices[0].message.content.strip())
        return {
            "supplierName": parsed.get("supplierName") or supplier_name,
            "rawText": parsed.get("rawText") or raw_text,
            "warnings": parsed.get("warnings") or warnings,
            "items": parsed.get("items") or items,
            "languagePolishSource": "llm",
            "languagePolishDurationMs": int((time.perf_counter() - polish_start) * 1000)
        }
    except Exception:
        return {
            "supplierName": supplier_name,
            "rawText": raw_text,
            "warnings": warnings,
            "items": items,
            "languagePolishSource": "llm_failed_fallback",
            "languagePolishDurationMs": int((time.perf_counter() - polish_start) * 1000)
        }

@app.post("/ocr/inbound/recognize")
async def recognize_inbound(file: UploadFile = File(...)):
    request_start = time.perf_counter()
    filename = file.filename or "unknown"
    suffix = Path(filename).suffix.lower()

    if suffix == ".pdf":
        return JSONResponse({
            "success": False,
            "message": "当前版本暂未接入PDF识别，下一步将支持PDF转图片后OCR",
            "data": None
        })

    if suffix not in SUPPORTED_IMAGE_SUFFIXES:
        return JSONResponse({
            "success": False,
            "message": f"暂不支持的文件类型: {suffix}",
            "data": None
        })

    temp_path = None
    try:
        content = await file.read()

        if not content:
            return JSONResponse({
                "success": False,
                "message": "上传文件内容为空，请重新选择图片后再试",
                "data": None
            })

        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
            tmp.write(content)
            tmp.flush()
            temp_path = tmp.name

        if not os.path.exists(temp_path) or os.path.getsize(temp_path) == 0:
            return JSONResponse({
                "success": False,
                "message": "临时图片文件写入失败，文件大小为 0",
                "data": None
            })

        ocr_start = time.perf_counter()
        result = ocr.predict(temp_path)
        ocr_duration_ms = int((time.perf_counter() - ocr_start) * 1000)

        texts = []
        for page in result:
            rec_texts = page.get("rec_texts", []) if isinstance(page, dict) else []
            texts.extend(rec_texts)

        raw_text = "\n".join(texts).strip()
        raw_text = normalize_text(raw_text)

        if not raw_text:
            return JSONResponse({
                "success": False,
                "message": "OCR未识别到有效文本",
                "data": None
            })

        try:
            llm_extract_start = time.perf_counter()
            llm_data = call_llm_extract_generic(raw_text)
            llm_extract_duration_ms = int((time.perf_counter() - llm_extract_start) * 1000)
            print("LLM data:", llm_data)

            supplier_name = llm_data.get("supplierName") or parse_supplier_name(raw_text)
            llm_raw_text = llm_data.get("rawText") or raw_text
            items = llm_data.get("items") or []
            warnings = llm_data.get("warnings") or []

            if not warnings:
                warnings = build_warnings(items)

            polished = ai_language_polish_result(
                raw_text=llm_raw_text,
                supplier_name=supplier_name,
                items=items,
                warnings=warnings
            )
            total_duration_ms = int((time.perf_counter() - request_start) * 1000)
            print(
                f"[recognize_inbound_timing] "
                f"ocr_ms={ocr_duration_ms}, "
                f"llm_extract_ms={llm_extract_duration_ms}, "
                f"language_polish_ms={polished.get('languagePolishDurationMs', 0)}, "
                f"total_ms={total_duration_ms}, "
                f"language_polish_source={polished.get('languagePolishSource')}"
            )

            return JSONResponse({
                "success": True,
                "message": "ok",
                "data": {
                    "supplierName": polished["supplierName"],
                    "rawText": polished["rawText"],
                    "warnings": polished["warnings"],
                    "items": polished["items"],
                    "languagePolishSource": polished.get("languagePolishSource"),
                    "timingMs": {
                        "ocr": ocr_duration_ms,
                        "llmExtract": llm_extract_duration_ms,
                        "languagePolish": polished.get("languagePolishDurationMs", 0),
                        "total": total_duration_ms
                    }
                }
            })
        except Exception as llm_error:
            llm_extract_duration_ms = -1
            supplier_name = parse_supplier_name(raw_text)
            items = parse_items(raw_text)
            warnings = build_warnings(items)

            polished = ai_language_polish_result(
                raw_text=raw_text,
                supplier_name=supplier_name,
                items=items,
                warnings=warnings
            )
            total_duration_ms = int((time.perf_counter() - request_start) * 1000)
            print(
                f"[recognize_inbound_timing] "
                f"ocr_ms={ocr_duration_ms}, "
                f"llm_extract_ms={llm_extract_duration_ms}, "
                f"language_polish_ms={polished.get('languagePolishDurationMs', 0)}, "
                f"total_ms={total_duration_ms}, "
                f"language_polish_source={polished.get('languagePolishSource')}, "
                f"llm_extract_fallback=true"
            )

            return JSONResponse({
                "success": True,
                "message": f"LLM解析失败，已回退到规则解析: {str(llm_error)}",
                "data": {
                    "supplierName": polished["supplierName"],
                    "rawText": polished["rawText"],
                    "warnings": polished["warnings"],
                    "items": polished["items"],
                    "languagePolishSource": polished.get("languagePolishSource"),
                    "timingMs": {
                        "ocr": ocr_duration_ms,
                        "llmExtract": llm_extract_duration_ms,
                        "languagePolish": polished.get("languagePolishDurationMs", 0),
                        "total": total_duration_ms
                    }
                }
            })

    except Exception as e:
        return JSONResponse({
            "success": False,
            "message": f"OCR处理失败: {str(e)}",
            "data": None
        })
    finally:
        if temp_path and os.path.exists(temp_path):
            os.remove(temp_path)

@app.post("/ocr/outbound/recognize")
async def recognize_outbound(file: UploadFile = File(...)):
    request_start = time.perf_counter()
    filename = file.filename or "unknown"
    suffix = Path(filename).suffix.lower()

    if suffix == ".pdf":
        return JSONResponse({
            "success": False,
            "message": "当前版本暂未接入PDF识别，下一步将支持PDF转图片后OCR",
            "data": None
        })

    if suffix not in SUPPORTED_IMAGE_SUFFIXES:
        return JSONResponse({
            "success": False,
            "message": f"暂不支持的文件类型: {suffix}",
            "data": None
        })

    temp_path = None
    try:
        content = await file.read()

        if not content:
            return JSONResponse({
                "success": False,
                "message": "上传文件内容为空，请重新选择图片后再试",
                "data": None
            })

        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
            tmp.write(content)
            tmp.flush()
            temp_path = tmp.name

        if not os.path.exists(temp_path) or os.path.getsize(temp_path) == 0:
            return JSONResponse({
                "success": False,
                "message": "临时图片文件写入失败，文件大小为 0",
                "data": None
            })

        ocr_start = time.perf_counter()
        result = ocr.predict(temp_path)
        ocr_duration_ms = int((time.perf_counter() - ocr_start) * 1000)

        texts = []
        for page in result:
            rec_texts = page.get("rec_texts", []) if isinstance(page, dict) else []
            texts.extend(rec_texts)

        raw_text = "\n".join(texts).strip()
        raw_text = normalize_text(raw_text)

        if not raw_text:
            return JSONResponse({
                "success": False,
                "message": "OCR未识别到有效文本",
                "data": None
            })

        try:
            llm_extract_start = time.perf_counter()
            llm_data = call_llm_extract_generic(raw_text)
            llm_extract_duration_ms = int((time.perf_counter() - llm_extract_start) * 1000)

            customer_name = llm_data.get("customerName") or llm_data.get("supplierName") or parse_customer_name(raw_text)
            llm_raw_text = llm_data.get("rawText") or raw_text
            items = llm_data.get("items") or []
            warnings = llm_data.get("warnings") or []

            if not warnings:
                warnings = build_warnings(items)

            polished = ai_language_polish_result(
                raw_text=llm_raw_text,
                supplier_name=customer_name,
                items=items,
                warnings=warnings
            )
            total_duration_ms = int((time.perf_counter() - request_start) * 1000)

            return JSONResponse({
                "success": True,
                "message": "ok",
                "data": {
                    "customerName": polished["supplierName"],
                    "rawText": polished["rawText"],
                    "warnings": polished["warnings"],
                    "items": polished["items"],
                    "languagePolishSource": polished.get("languagePolishSource"),
                    "timingMs": {
                        "ocr": ocr_duration_ms,
                        "llmExtract": llm_extract_duration_ms,
                        "languagePolish": polished.get("languagePolishDurationMs", 0),
                        "total": total_duration_ms
                    }
                }
            })
        except Exception as llm_error:
            llm_extract_duration_ms = -1
            customer_name = parse_customer_name(raw_text)
            items = parse_items(raw_text)
            warnings = build_warnings(items)

            polished = ai_language_polish_result(
                raw_text=raw_text,
                supplier_name=customer_name,
                items=items,
                warnings=warnings
            )
            total_duration_ms = int((time.perf_counter() - request_start) * 1000)

            return JSONResponse({
                "success": True,
                "message": f"LLM解析失败，已回退到规则解析: {str(llm_error)}",
                "data": {
                    "customerName": polished["supplierName"],
                    "rawText": polished["rawText"],
                    "warnings": polished["warnings"],
                    "items": polished["items"],
                    "languagePolishSource": polished.get("languagePolishSource"),
                    "timingMs": {
                        "ocr": ocr_duration_ms,
                        "llmExtract": llm_extract_duration_ms,
                        "languagePolish": polished.get("languagePolishDurationMs", 0),
                        "total": total_duration_ms
                    }
                }
            })

    except Exception as e:
        return JSONResponse({
            "success": False,
            "message": f"OCR处理失败: {str(e)}",
            "data": None
        })
    finally:
        if temp_path and os.path.exists(temp_path):
            os.remove(temp_path)

def call_llm_extract(raw_text: str) -> Dict:
    if not client:
        raise RuntimeError("未配置 DEEPSEEK_API_KEY，无法调用大模型")

    prompt = f"""
你是仓库入库单识别助手。
请根据下面的 OCR 文本，提取入库单结构化信息。
只返回 JSON，不要返回解释，不要加 markdown 代码块。

要求输出字段：
{{
  "supplierName": "字符串，没有就填未识别供应商",
  "rawText": "原始文本原样返回",
  "warnings": ["字符串数组"],
  "items": [
    {{
      "lineNo": 1,
      "productName": "商品名称",
      "specification": "规格，没有就空字符串",
      "unit": "单位，没有就空字符串",
      "quantity": 0,
      "unitPrice": 0,
      "amount": 0
    }}
  ]
}}

规则：
1. quantity 必须是数字，优先整数。
2. unitPrice 和 amount 必须是数字。
3. 如果 amount 缺失，可按 quantity * unitPrice 计算，保留两位小数。
4. 若某行无法确定，请不要乱编，写入 warnings。
5. rawText 直接返回我给你的 OCR 文本。
6. 返回内容必须是合法 json。
7. 对常见OCR错字进行业务纠正，例如：
- “供应肉”应识别为“供应商”
- “尿号”应识别为“序号”
- “奶恪”应识别为“规格”
- “我”应识别为“数量”
- “金”应识别为“金额”
- “俄”在单位场景下优先判断为“根”
- “三遇换头”优先纠正为“三通换头”或“三通接头”，结合上下文选择更合理者
8. 在能高置信判断时，直接输出纠正后的标准字段，不要只写在 warnings 中。
9. warnings 只保留真正需要人工确认的问题，不要输出过长解释。
10. 商品明细优先保证 productName、unit、quantity、unitPrice、amount 的业务可用性。

OCR文本如下：
{raw_text}
"""

    response = client.chat.completions.create(
        model=DEEPSEEK_MODEL,
        messages=[
            {"role": "system", "content": "你是仓库入库单识别助手，只返回合法 JSON。"},
            {"role": "user", "content": prompt}
        ],
        response_format={"type": "json_object"}
    )

    print("LLM raw response:", response.choices[0].message.content)
    text = response.choices[0].message.content.strip()
    return json.loads(text)


def call_llm_extract_generic(raw_text: str) -> Dict:
    if not client:
        raise RuntimeError("未配置 DEEPSEEK_API_KEY，无法调用大模型")

    prompt = f"""
你是仓库入库单结构化抽取助手。
请基于 OCR 文本输出标准 JSON。只返回 JSON，不要解释，不要 markdown。

输入 OCR 文本：
{raw_text}

输出 JSON 字段（必须一致）：
{{
  "supplierName": "string，无法识别填 '未识别供应商'",
  "rawText": "string，返回纠正后的可读文本",
  "warnings": ["string"],
  "items": [
    {{
      "lineNo": 1,
      "productName": "string",
      "specification": "string",
      "unit": "string",
      "quantity": 0,
      "unitPrice": 0,
      "amount": 0
    }}
  ]
}}

规则：
1. 先做 OCR 噪声纠正，再做结构化抽取。
2. 不确定信息不要编造，写入 warnings。
3. quantity/unitPrice/amount 必须是数字；amount 缺失时按 quantity * unitPrice 计算，保留两位小数。
4. specification/unit 识别不到时填空字符串，不要填 null。
5. lineNo 从 1 开始递增。
6. warnings 仅保留需要人工确认的问题。
7. 输出必须是可解析 JSON。
"""

    response = client.chat.completions.create(
        model=DEEPSEEK_MODEL,
        messages=[
            {"role": "system", "content": "你是结构化抽取助手，只返回合法 JSON。"},
            {"role": "user", "content": prompt}
        ],
        response_format={"type": "json_object"}
    )

    print("LLM raw response (generic):", response.choices[0].message.content)
    text = response.choices[0].message.content.strip()
    return json.loads(text)
