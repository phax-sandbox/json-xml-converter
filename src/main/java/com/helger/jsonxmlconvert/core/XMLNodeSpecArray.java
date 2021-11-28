package com.helger.jsonxmlconvert.core;

import org.json.JSONObject;

public class XMLNodeSpecArray extends AbstractXMLNodeSpec
{
  private String m_sItemsRef;
  private AbstractXMLNodeSpec m_aItemsXMLNodeSpec;

  public XMLNodeSpecArray ()
  {
    super (TYPE_ARRAY);
  }

  public String getItemsRef ()
  {
    return m_sItemsRef;
  }

  public AbstractXMLNodeSpec getItemsXMLNodeSpec ()
  {
    return m_aItemsXMLNodeSpec;
  }

  public void setItemsXMLNodeSpec (final AbstractXMLNodeSpec itemsXMLNodeSpec)
  {
    this.m_aItemsXMLNodeSpec = itemsXMLNodeSpec;
  }

  public boolean isXMLWrapped ()
  {
    return m_bIsXMLWrapped;
  }

  public String calculateArrayTargetFullXMLName (final String objectName)
  {
    // TODO: test cases with XMLNodeSpec in items object must be provided

    // need to look at XMLNodeSpec of inner object
    if (m_aItemsXMLNodeSpec != null)
      return m_aItemsXMLNodeSpec.calculateTargetFullXMLName (objectName);

    // PATCHED: use outer XMLNodeSpec to calculate full XML names
    return super.calculateTargetFullXMLName (objectName);
  }

  @Override
  public void loadJSONValue (final JSONObject schema, final String valueDesriptionForException) throws JSONSchemaLoadException
  {
    super.loadJSONValue (schema, valueDesriptionForException);
    final JSONObject items = schema.optJSONObject ("items");
    if (items == null)
      throw new JSONSchemaLoadException ("Items can not be found for object '" + valueDesriptionForException + "'");

    // check for $ref
    final String ref = items.optString ("$ref", null);
    if (ref != null)
    {
      m_sItemsRef = ref;
      return;
    }

    final String itemsType = items.optString ("type", null);
    if (itemsType == null)
      throw new JSONSchemaLoadException ("Type is not defined for items of object '" + valueDesriptionForException + "'");
    if (itemsType.equals ("object"))
    {
      m_aItemsXMLNodeSpec = new XMLNodeSpecObject ();
      m_aItemsXMLNodeSpec.loadJSONValue (items, valueDesriptionForException);
      // System.out.println("added items object");
      return;
    }
    if (itemsType.equals ("array"))
    {
      final XMLNodeSpecArray itemsXMLNodeSpecArray = new XMLNodeSpecArray ();
      m_aItemsXMLNodeSpec = itemsXMLNodeSpecArray;
      itemsXMLNodeSpecArray.loadJSONValue (items, valueDesriptionForException);
      // check that items array is wrapped
      if (!itemsXMLNodeSpecArray.isXMLWrapped ())
        throw new JSONSchemaLoadException ("Nested array should always be wrapped for array '" + valueDesriptionForException + "'");
      // System.out.println("added items array");
      return;
    }

    final int type = typeStringToTYPE (itemsType);
    m_aItemsXMLNodeSpec = new XMLNodeSpecPrimitiveType (type);
    m_aItemsXMLNodeSpec.loadJSONValue (items, valueDesriptionForException);
    // System.out.println("added items simple value");
  }
}
