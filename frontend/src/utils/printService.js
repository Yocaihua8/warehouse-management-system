export const triggerBrowserPrint = async (delayMs = 200) => {
  if (typeof window === 'undefined') {
    return
  }
  await new Promise((resolve) => {
    window.setTimeout(resolve, delayMs)
  })
  window.print()
}
