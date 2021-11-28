package com.helger.jsonxmlconvert.cache;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.jsonxmlconvert.core.JSONSchemaForXML;
import com.helger.jsonxmlconvert.core.JSONSchemaLoadException;

public class SchemaCache
{
  // standard 60 seconds
  private static long s_nAgeScanFrequency = 60 * 1000;
  // default no aging
  private static long s_nCacheMaxAge = -1;
  // default 128KB
  private static int s_nMaxDownloadSize = 128 * 1024;
  // default 128 entries
  private static int s_nMaxEntries = 128;

  private static final Logger LOGGER = LoggerFactory.getLogger (SchemaCache.class.getName ());

  private static final SchemaCache INSTANCE = new SchemaCache ("singleton");

  private final String m_sCacheName;
  private Map <String, JSONSchemaForXML> jsonSchemaForXMLCache;
  private int m_nTotalDownloadSize;
  private long m_nLastScanTimeInMs;

  public static SchemaCache getSingleton ()
  {
    return INSTANCE;
  }

  /**
   * @param jsonxmlSchemaCacheMaxAge
   *        in ms, -1 for no aging
   */
  public static void setJsonxmlSchemaCacheMaxAge (final long jsonxmlSchemaCacheMaxAge)
  {
    if (jsonxmlSchemaCacheMaxAge < 1 && jsonxmlSchemaCacheMaxAge != -1)
      return;
    SchemaCache.s_nCacheMaxAge = jsonxmlSchemaCacheMaxAge;
  }

  public static void setJsonxmlSchemaCacheMaxDownloadSize (final int jsonxmlSchemaCacheMaxDownloadSize)
  {
    if (jsonxmlSchemaCacheMaxDownloadSize < 1)
      return;
    SchemaCache.s_nMaxDownloadSize = jsonxmlSchemaCacheMaxDownloadSize;
  }

  public static void setJsonxmlSchemaCacheMaxEntries (final int jsonxmlSchemaCacheMaxEntries)
  {
    if (jsonxmlSchemaCacheMaxEntries < 0)
      return;
    SchemaCache.s_nMaxEntries = jsonxmlSchemaCacheMaxEntries;
  }

  public static void setJSONXML_SCHEMA_CACHE_AGE_SCAN_FREQUENCY (final long l)
  {
    s_nAgeScanFrequency = l;
  }

  public SchemaCache (final String cacheName)
  {
    this.m_sCacheName = cacheName;
    flushCache ();
  }

  public synchronized void flushCache ()
  {
    LOGGER.debug ("[" + m_sCacheName + "] Flushing cache");
    jsonSchemaForXMLCache = new HashMap <> (s_nMaxEntries);
    m_nTotalDownloadSize = 0;
    m_nLastScanTimeInMs = System.currentTimeMillis ();
  }

  private synchronized void removeOldCache ()
  {
    if (s_nCacheMaxAge == -1)
      return;
    int removedKeys = 0;
    final long now = System.currentTimeMillis ();
    if (m_nLastScanTimeInMs > (now - s_nAgeScanFrequency))
      return;
    final long pointInTime = now - s_nCacheMaxAge;
    for (final String schemaKey : jsonSchemaForXMLCache.keySet ())
    {
      final JSONSchemaForXML jsonSchemaForXML = jsonSchemaForXMLCache.get (schemaKey);
      if (jsonSchemaForXML.getCreatedTimeInMs () < pointInTime)
      {
        jsonSchemaForXMLCache.remove (schemaKey);
        m_nTotalDownloadSize -= schemaKey.length ();
        removedKeys++;
      }
    }
    m_nLastScanTimeInMs = now;
    if (LOGGER.isDebugEnabled ())
    {
      LOGGER.debug ("[" + m_sCacheName + "] Removed " + removedKeys + " old entries");
      LOGGER.debug ("[" +
                    m_sCacheName +
                    "] Current size: " +
                    m_nTotalDownloadSize +
                    " chars (max " +
                    s_nMaxDownloadSize +
                    "), " +
                    jsonSchemaForXMLCache.size () +
                    " entries (max " +
                    s_nMaxEntries +
                    "), cache max age: " +
                    s_nCacheMaxAge +
                    " ms");
    }
  }

  public synchronized JSONSchemaForXML getJSONSchemaForXML (final String jsonSchema) throws JSONSchemaLoadException
  {
    removeOldCache ();
    JSONSchemaForXML jsonSchemaForXML = jsonSchemaForXMLCache.get (jsonSchema);
    if (jsonSchemaForXML != null)
      return jsonSchemaForXML;
    jsonSchemaForXML = new JSONSchemaForXML (jsonSchema);
    final int jsonSchemaLength = jsonSchema.length ();
    if (m_nTotalDownloadSize + jsonSchemaLength > s_nMaxDownloadSize)
    {
      // cache byte size exceeded
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Cache byte size exceeded for cache " + m_sCacheName);
      return jsonSchemaForXML;
    }
    if ((jsonSchemaForXMLCache.size () + 1) > s_nMaxEntries)
    {
      // cache entry size exceeded
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Cache entry size exceeded for cache " + m_sCacheName);
      return jsonSchemaForXML;
    }
    jsonSchemaForXMLCache.put (jsonSchema, jsonSchemaForXML);
    m_nTotalDownloadSize += jsonSchemaLength;
    return jsonSchemaForXML;
  }

}
