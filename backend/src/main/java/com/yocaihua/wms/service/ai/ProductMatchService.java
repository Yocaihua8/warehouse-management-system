package com.yocaihua.wms.service.ai;

import com.yocaihua.wms.entity.Customer;
import com.yocaihua.wms.entity.Product;
import com.yocaihua.wms.entity.Supplier;
import com.yocaihua.wms.mapper.CustomerMapper;
import com.yocaihua.wms.mapper.ProductMapper;
import com.yocaihua.wms.mapper.SupplierMapper;
import com.yocaihua.wms.vo.AiInboundRecognizeItemVO;
import com.yocaihua.wms.vo.AiOutboundRecognizeItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductMatchService {

    private final ProductMapper productMapper;
    private final CustomerMapper customerMapper;
    private final SupplierMapper supplierMapper;

    @Value("${ai.match.product.min-score:0.72}")
    private double productMatchMinScore;

    @Value("${ai.match.product.ambiguity-gap:0.05}")
    private double productMatchAmbiguityGap;

    @Value("${ai.match.product.levenshtein-weight:0.65}")
    private double levenshteinWeight;

    @Value("${ai.match.product.ngram-weight:0.35}")
    private double ngramWeight;

    @Value("${ai.match.product.candidate-limit:500}")
    private int candidateLimit;

    public void matchInboundProducts(List<AiInboundRecognizeItemVO> itemVOList) {
        for (AiInboundRecognizeItemVO item : itemVOList) {
            ProductMatchResult matchResult = findMatchedProduct(
                    item.getProductName(),
                    item.getSpecification(),
                    item.getUnit()
            );

            item.setMatchedProductId(matchResult.getProductId());
            item.setMatchStatus(matchResult.getMatchStatus());

            String oldRemark = item.getRemark() == null ? "" : item.getRemark();
            String newRemark = matchResult.getRemark();
            if (newRemark != null && !newRemark.isBlank()) {
                item.setRemark(oldRemark.isBlank() ? newRemark : oldRemark + "；" + newRemark);
            }
        }
    }

    public void matchOutboundProducts(List<AiOutboundRecognizeItemVO> itemVOList) {
        for (AiOutboundRecognizeItemVO item : itemVOList) {
            ProductMatchResult matchResult = findMatchedProduct(
                    item.getProductName(),
                    item.getSpecification(),
                    item.getUnit()
            );

            item.setMatchedProductId(matchResult.getProductId());
            item.setMatchStatus(matchResult.getMatchStatus());

            String oldRemark = item.getRemark() == null ? "" : item.getRemark();
            String newRemark = matchResult.getRemark();
            if (newRemark != null && !newRemark.isBlank()) {
                item.setRemark(oldRemark.isBlank() ? newRemark : oldRemark + "；" + newRemark);
            }
        }
    }

    public SupplierMatchResult findMatchedSupplier(String supplierName) {
        if (supplierName == null || supplierName.trim().isEmpty()) {
            return new SupplierMatchResult(null, "unmatched", "未识别到供应商");
        }

        String normalizedName = normalizeText(supplierName);
        Supplier exactSupplier = supplierMapper.selectByName(normalizedName);
        if (exactSupplier != null) {
            return new SupplierMatchResult(exactSupplier.getId(), "matched_exact", "按供应商名称精确匹配");
        }

        List<Supplier> fuzzyList = supplierMapper.selectByNameLike(normalizedName);
        if (fuzzyList != null && fuzzyList.size() == 1) {
            return new SupplierMatchResult(fuzzyList.get(0).getId(), "matched_fuzzy", "按供应商名称模糊匹配");
        }
        if (fuzzyList != null && fuzzyList.size() > 1) {
            return new SupplierMatchResult(null, "unmatched", "存在多个模糊匹配供应商，请人工确认");
        }
        return new SupplierMatchResult(null, "unmatched", "未匹配到系统供应商");
    }

    public CustomerMatchResult findMatchedCustomer(String customerName) {
        if (customerName == null || customerName.trim().isEmpty() || "未识别客户".equals(customerName)) {
            return new CustomerMatchResult(null, "unmatched", "未识别到客户");
        }

        String normalizedName = normalizeText(customerName);
        Customer exactCustomer = customerMapper.selectByName(normalizedName);
        if (exactCustomer != null) {
            return new CustomerMatchResult(exactCustomer.getId(), "matched_exact", "按客户名称精确匹配");
        }

        List<Customer> fuzzyList = customerMapper.selectByNameLike(normalizedName);
        if (fuzzyList != null && fuzzyList.size() == 1) {
            return new CustomerMatchResult(fuzzyList.get(0).getId(), "matched_fuzzy", "按客户名称模糊匹配");
        }
        if (fuzzyList != null && fuzzyList.size() > 1) {
            return new CustomerMatchResult(null, "unmatched", "存在多个模糊匹配客户，请人工确认");
        }
        return new CustomerMatchResult(null, "unmatched", "未匹配到系统客户");
    }

    private ProductMatchResult findMatchedProduct(String productName, String specification, String unit) {
        if (productName == null || productName.trim().isEmpty()) {
            return new ProductMatchResult(null, "unmatched", "商品名称为空，无法匹配");
        }

        String normalizedName = normalizeText(productName);
        String normalizedSpec = normalizeText(specification);
        String normalizedUnit = normalizeText(unit);

        if (!normalizedSpec.isEmpty() && !normalizedUnit.isEmpty()) {
            Product exactProduct = productMapper.selectByNameSpecUnit(normalizedName, normalizedSpec, normalizedUnit);
            if (exactProduct != null) {
                return new ProductMatchResult(exactProduct.getId(), "matched_exact", "按商品名+规格+单位精确匹配");
            }
        }

        if (!normalizedSpec.isEmpty()) {
            Product nameSpecProduct = productMapper.selectByNameSpec(normalizedName, normalizedSpec);
            if (nameSpecProduct != null) {
                return new ProductMatchResult(nameSpecProduct.getId(), "matched_name_spec", "按商品名+规格匹配");
            }
        }

        Product nameProduct = productMapper.selectByName(normalizedName);
        if (nameProduct != null) {
            return new ProductMatchResult(nameProduct.getId(), "matched_name", "按商品名匹配");
        }

        return findSemanticMatchedProduct(normalizedName, normalizedSpec, normalizedUnit);
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().replace(" ", "");
    }

    private ProductMatchResult findSemanticMatchedProduct(String normalizedName, String normalizedSpec, String normalizedUnit) {
        List<Product> candidates = productMapper.selectAiCandidates(candidateLimit);
        if (candidates == null || candidates.isEmpty()) {
            return new ProductMatchResult(null, "unmatched", "未匹配到系统商品");
        }

        String queryVectorText = buildVectorText(normalizedName, normalizedSpec, normalizedUnit);
        List<ProductScore> scoredList = new ArrayList<>();
        for (Product product : candidates) {
            String candidateName = normalizeText(product.getProductName());
            if (candidateName.isEmpty()) {
                continue;
            }

            double nameLevenshtein = levenshteinSimilarity(normalizedName, candidateName);
            String candidateVectorText = buildVectorText(
                    candidateName,
                    normalizeText(product.getSpecification()),
                    normalizeText(product.getUnit())
            );
            double vectorSimilarity = trigramCosineSimilarity(queryVectorText, candidateVectorText);

            double weightedScore = (levenshteinWeight * nameLevenshtein) + (ngramWeight * vectorSimilarity);
            if (!normalizedSpec.isEmpty() && normalizedSpec.equals(normalizeText(product.getSpecification()))) {
                weightedScore += 0.08D;
            }
            if (!normalizedUnit.isEmpty() && normalizedUnit.equals(normalizeText(product.getUnit()))) {
                weightedScore += 0.05D;
            }
            double finalScore = Math.min(1.0D, weightedScore);
            scoredList.add(new ProductScore(product, finalScore, nameLevenshtein, vectorSimilarity));
        }

        if (scoredList.isEmpty()) {
            return new ProductMatchResult(null, "unmatched", "未匹配到系统商品");
        }

        scoredList.sort(Comparator.comparing(ProductScore::score).reversed());
        ProductScore best = scoredList.get(0);
        if (best.score() < productMatchMinScore) {
            return new ProductMatchResult(
                    null,
                    "unmatched",
                    String.format("未匹配到系统商品（最高相似度 %.2f）", best.score())
            );
        }

        if (scoredList.size() > 1) {
            ProductScore second = scoredList.get(1);
            if (second.score() >= productMatchMinScore && (best.score() - second.score()) < productMatchAmbiguityGap) {
                return new ProductMatchResult(
                        null,
                        "unmatched",
                        String.format("存在多个高相似商品（%.2f/%.2f），请人工确认", best.score(), second.score())
                );
            }
        }

        return new ProductMatchResult(
                best.product().getId(),
                "matched_fuzzy",
                String.format(
                        "按语义相似匹配（score=%.2f, lev=%.2f, vec=%.2f）",
                        best.score(),
                        best.levenshtein(),
                        best.vector()
                )
        );
    }

    private String buildVectorText(String name, String specification, String unit) {
        StringBuilder sb = new StringBuilder();
        sb.append(name == null ? "" : name);
        if (specification != null && !specification.isEmpty()) {
            sb.append('|').append(specification);
        }
        if (unit != null && !unit.isEmpty()) {
            sb.append('|').append(unit);
        }
        return sb.toString();
    }

    private double levenshteinSimilarity(String left, String right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return 0D;
        }
        if (left.equals(right)) {
            return 1D;
        }
        int distance = levenshteinDistance(left, right);
        int maxLen = Math.max(left.length(), right.length());
        if (maxLen == 0) {
            return 1D;
        }
        return 1D - ((double) distance / maxLen);
    }

    private int levenshteinDistance(String left, String right) {
        int n = left.length();
        int m = right.length();
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];

        for (int j = 0; j <= m; j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            char c1 = left.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                int cost = c1 == right.charAt(j - 1) ? 0 : 1;
                int insert = curr[j - 1] + 1;
                int delete = prev[j] + 1;
                int replace = prev[j - 1] + cost;
                curr[j] = Math.min(Math.min(insert, delete), replace);
            }
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }
        return prev[m];
    }

    private double trigramCosineSimilarity(String left, String right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return 0D;
        }
        Map<String, Integer> leftVector = trigramFrequency(left);
        Map<String, Integer> rightVector = trigramFrequency(right);
        if (leftVector.isEmpty() || rightVector.isEmpty()) {
            return 0D;
        }

        double dot = 0D;
        for (Map.Entry<String, Integer> entry : leftVector.entrySet()) {
            Integer rv = rightVector.get(entry.getKey());
            if (rv != null) {
                dot += entry.getValue() * rv;
            }
        }

        double leftNorm = 0D;
        for (Integer value : leftVector.values()) {
            leftNorm += value * value;
        }
        double rightNorm = 0D;
        for (Integer value : rightVector.values()) {
            rightNorm += value * value;
        }
        if (leftNorm == 0D || rightNorm == 0D) {
            return 0D;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private Map<String, Integer> trigramFrequency(String text) {
        Map<String, Integer> map = new HashMap<>();
        String normalized = normalizeText(text);
        if (normalized.isEmpty()) {
            return map;
        }

        String padded = "^" + normalized + "$";
        if (padded.length() < 3) {
            map.put(padded, 1);
            return map;
        }
        for (int i = 0; i <= padded.length() - 3; i++) {
            String token = padded.substring(i, i + 3);
            map.put(token, map.getOrDefault(token, 0) + 1);
        }
        return map;
    }

    private record ProductScore(Product product, double score, double levenshtein, double vector) {
    }

    private static class ProductMatchResult {
        private final Long productId;
        private final String matchStatus;
        private final String remark;

        private ProductMatchResult(Long productId, String matchStatus, String remark) {
            this.productId = productId;
            this.matchStatus = matchStatus;
            this.remark = remark;
        }

        public Long getProductId() {
            return productId;
        }

        public String getMatchStatus() {
            return matchStatus;
        }

        public String getRemark() {
            return remark;
        }
    }

    public static final class CustomerMatchResult {
        private final Long customerId;
        private final String matchStatus;
        private final String remark;

        public CustomerMatchResult(Long customerId, String matchStatus, String remark) {
            this.customerId = customerId;
            this.matchStatus = matchStatus;
            this.remark = remark;
        }

        public Long getCustomerId() {
            return customerId;
        }

        public String getMatchStatus() {
            return matchStatus;
        }

        public String getRemark() {
            return remark;
        }
    }

    public static final class SupplierMatchResult {
        private final Long supplierId;
        private final String matchStatus;
        private final String remark;

        public SupplierMatchResult(Long supplierId, String matchStatus, String remark) {
            this.supplierId = supplierId;
            this.matchStatus = matchStatus;
            this.remark = remark;
        }

        public Long getSupplierId() {
            return supplierId;
        }

        public String getMatchStatus() {
            return matchStatus;
        }

        public String getRemark() {
            return remark;
        }
    }
}
