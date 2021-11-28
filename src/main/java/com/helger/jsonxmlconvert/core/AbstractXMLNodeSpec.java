package com.helger.jsonxmlconvert.core;

import org.json.JSONObject;

public abstract class AbstractXMLNodeSpec
{
  private static final int TYPE_UNDEFINED = 0;
  static final int TYPE_OBJECT = 1;
  static final int TYPE_ARRAY = 2;
  static final int TYPE_STRING = 3;
  static final int TYPE_NUMBER = 4;
  static final int TYPE_INTEGER = 5;
  static final int TYPE_BOOLEAN = 6;
  static final int TYPE_OBJECT_WITH_TEXT_AND_ATTRIBUTES = 7;

  protected int m_nNodeType = TYPE_UNDEFINED;
  protected boolean m_bIsNullable = false;
  protected String m_sXmlName;
  protected String m_sXmlPrefix;
  protected String m_sXmlNamespace;
  protected boolean m_bIsXMLAttribute = false;
  protected boolean m_bIsXMLWrapped = false;
  protected boolean m_bIsXMLTextContent = false;
  protected boolean m_bIsXMLTextContentParent = false;

  protected static int typeStringToTYPE (final String type) throws JSONSchemaLoadException
  {
    if (type == null)
      throw new JSONSchemaLoadException ("JSON schema type is null / undefined");
    if (type.equals ("object"))
      return TYPE_OBJECT;
    if (type.equals ("array"))
      return TYPE_ARRAY;
    if (type.equals ("string"))
      return TYPE_STRING;
    if (type.equals ("number"))
      return TYPE_NUMBER;
    if (type.equals ("integer"))
      return TYPE_INTEGER;
    if (type.equals ("boolean"))
      return TYPE_BOOLEAN;
    throw new JSONSchemaLoadException ("Unknow JSON schema type '" + type + "'");
  }

  public boolean isPrimitiveType ()
  {
    return false;
  }

  public boolean isNullable ()
  {
    return m_bIsNullable;
  }

  public boolean isXMLTextContentParent ()
  {
    return m_bIsXMLTextContentParent;
  }

  public void setXMLTextContentParent (final boolean isXMLTextContentParent)
  {
    this.m_bIsXMLTextContentParent = isXMLTextContentParent;
  }

  public AbstractXMLNodeSpec (final int nodeType)
  {
    this.m_nNodeType = nodeType;
  }

  public int getNodeType ()
  {
    return m_nNodeType;
  }

  public String calculateTargetFullXMLName (final String objectName)
  {
    final String name = (m_sXmlName == null ? objectName : m_sXmlName);
    final String fullXMLName = (m_sXmlPrefix != null && m_sXmlPrefix.length () > 0 ? m_sXmlPrefix + ":" + name : name);
    return fullXMLName;
  }

  public String getXmlName ()
  {
    return m_sXmlName;
  }

  public String getXmlPrefix ()
  {
    return m_sXmlPrefix;
  }

  public String getXmlNamespace ()
  {
    return m_sXmlNamespace;
  }

  private void _loadXML (final JSONObject xml)
  {
    if (xml == null)
      return;
    m_sXmlName = xml.optString ("name", null);
    if (m_sXmlName != null && m_sXmlName.length () == 0)
    {
      m_sXmlName = null;
    }
    m_sXmlPrefix = xml.optString ("prefix", null);
    if (m_sXmlPrefix != null && m_sXmlPrefix.length () == 0)
    {
      m_sXmlPrefix = null;
    }
    m_sXmlNamespace = xml.optString ("namespace", null);
    if (m_sXmlNamespace != null && m_sXmlNamespace.length () == 0)
    {
      m_sXmlNamespace = null;
    }
    m_bIsXMLAttribute = xml.optBoolean ("attribute", false);
    m_bIsXMLWrapped = xml.optBoolean ("wrapped", false);
    m_bIsXMLTextContent = xml.optBoolean ("textcontent", false);
  }

  public void loadJSONValue (final JSONObject schema, final String valueDesriptionForException) throws JSONSchemaLoadException
  {
    m_bIsNullable = schema.optBoolean ("nullable");
    _loadXML (schema.optJSONObject ("xml"));
  }

}
