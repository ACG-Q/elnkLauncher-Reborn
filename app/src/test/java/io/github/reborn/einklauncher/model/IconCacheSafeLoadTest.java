package io.github.reborn.einklauncher.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * {@link IconCache#safeLoad} 的兜底行为测试：异常、null 结果均不外抛并返回兜底值。
 */
public class IconCacheSafeLoadTest {

  @Test
  public void testSafeLoadReturnsFallbackWhenLoaderThrows() {
    String result = IconCache.safeLoad("com.example.app", () -> {
      throw new NullPointerException("broken icon");
    }, "fallback");
    assertEquals("fallback", result);
  }

  @Test
  public void testSafeLoadReturnsValueWhenLoaderSucceeds() {
    String result = IconCache.safeLoad("com.example.app", () -> "value", "fallback");
    assertEquals("value", result);
  }

  @Test
  public void testSafeLoadReturnsFallbackWhenLoaderReturnsNull() {
    String result = IconCache.safeLoad("com.example.app", () -> null, "fallback");
    assertEquals("fallback", result);
  }
}
