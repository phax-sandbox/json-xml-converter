package com.helger.jsonxmlconvert.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONObject;

public class XMLNodeSpecObject extends AbstractXMLNodeSpec
{
  private final Map <String, PropertyXMLNodeSpec> m_aXmlElements = new HashMap <> ();
  private final Map <String, PropertyXMLNodeSpec> m_aXmlAttributes = new HashMap <> ();
  private final Map <String, AbstractXMLNodeSpec> m_aJsonProperties = new HashMap <> ();
  private final List <String> m_aNonWrappedJSONArrayNames = new ArrayList <> ();
  private final Map <String, PropertyXMLNodeSpec> m_aRefs = new HashMap <> ();
  private boolean m_bIsAdditionalProperties = true;

  public XMLNodeSpecObject ()
  {
    super (TYPE_OBJECT);
  }

  @Override
  public void loadJSONValue (final JSONObject schema, final String valueDesriptionForException) throws JSONSchemaLoadException
  {
    super.loadJSONValue (schema, valueDesriptionForException);
    // load additionalProperties. True by default, set to false if explicitely
    // included
    final Object additionalPropertiesObject = schema.opt ("additionalProperties");
    if (additionalPropertiesObject instanceof Boolean)
      m_bIsAdditionalProperties = (Boolean) additionalPropertiesObject;

    // load all properties
    final JSONObject properties = schema.optJSONObject ("properties");
    if (properties == null)
      // throw new JSONSchemaLoadException("Properties can not be found for
      // object '"
      // + valueDesriptionForException + "'");
      return;

    // count properties which are declared as text content
    // when 0 then return false
    // when 1 then return true
    // when > 1 then throw exception //not a simple type

    int countTextContentProperties = 0;

    for (final String propertyName : properties.keySet ())
    {
      if (propertyName.trim ().length () == 0)
        throw new JSONSchemaLoadException ("One property name (after trim) is empty for object '" + valueDesriptionForException + "'");
      final JSONObject propertyObject = properties.getJSONObject (propertyName);
      // check for $ref
      final String ref = propertyObject.optString ("$ref", null);
      if (ref != null)
      {
        m_aRefs.put (propertyName, new PropertyXMLNodeSpec (propertyName, ref, null));
        continue;
      }
      final String propertyType = propertyObject.optString ("type", null);
      if (propertyType == null)
        throw new JSONSchemaLoadException ("Type is not defined for property '" +
                                           propertyName +
                                           "' for object '" +
                                           valueDesriptionForException +
                                           "'");
      if (propertyType.equals ("object"))
      {
        final XMLNodeSpecObject innerObject = new XMLNodeSpecObject ();

        innerObject.loadJSONValue (propertyObject, propertyName);

        final PropertyXMLNodeSpec propertyXMLNodeSpec = new PropertyXMLNodeSpec (propertyName, null, innerObject);
        // String hashKey = calculateTargetXMLNodeName(propertyName,
        // innerObject.getFullXMLName());
        final String innerObjectXMLElementName = innerObject.calculateTargetFullXMLName (propertyName);
        _checkKeyNotExistInHashMap (innerObjectXMLElementName, m_aXmlElements, valueDesriptionForException + "." + propertyName);
        m_aXmlElements.put (innerObjectXMLElementName, propertyXMLNodeSpec);
        m_aJsonProperties.put (propertyName, innerObject);
        // System.out
        // System.out.println("added object XML element " +
        // innerObjectXMLElementName +
        // " to " + valueDesriptionForException);
        continue;
      }
      if (propertyType.equals ("array"))
      {
        final XMLNodeSpecArray innerObject = new XMLNodeSpecArray ();
        innerObject.loadJSONValue (propertyObject, propertyName);
        final PropertyXMLNodeSpec propertyXMLNodeSpec = new PropertyXMLNodeSpec (propertyName, null, innerObject);
        final String innerObjectXMLElementName = innerObject.calculateTargetFullXMLName (propertyName);
        _checkKeyNotExistInHashMap (innerObjectXMLElementName, m_aXmlElements, valueDesriptionForException + "." + propertyName);
        m_aXmlElements.put (innerObjectXMLElementName, propertyXMLNodeSpec);
        m_aJsonProperties.put (propertyName, innerObject);
        // System.out.println("added array XML element " +
        // innerObjectXMLElementName + "
        // to " + valueDesriptionForException);
        if (!innerObject.isXMLWrapped ())
          m_aNonWrappedJSONArrayNames.add (propertyName);
        continue;
      }
      final int type = typeStringToTYPE (propertyType);
      final XMLNodeSpecPrimitiveType innerObject = new XMLNodeSpecPrimitiveType (type);
      innerObject.loadJSONPrimitiveValue (propertyObject, propertyName);

      // Parent is XML SimpleType with attributes
      if (innerObject.isXMLTextContent ())
      {
        countTextContentProperties++;
      }

      final PropertyXMLNodeSpec propertyXMLNodeSpec = new PropertyXMLNodeSpec (propertyName, null, innerObject);
      // String hashKey = calculateTargetXMLNodeName(propertyName,
      // innerObject.getFullXMLName());
      final String innerObjectXMLElementName = innerObject.calculateTargetFullXMLName (propertyName);
      if (innerObject.isXMLAttribute ())
      {
        _checkKeyNotExistInHashMap (innerObjectXMLElementName, m_aXmlAttributes, valueDesriptionForException + "." + propertyName);
        m_aXmlAttributes.put (innerObjectXMLElementName, propertyXMLNodeSpec);
        // System.out.println("added simple value XML attribute " +
        // innerObjectXMLElementName + " to " + valueDesriptionForException);
      }
      else
      {
        _checkKeyNotExistInHashMap (innerObjectXMLElementName, m_aXmlElements, valueDesriptionForException + "." + propertyName);
        m_aXmlElements.put (innerObjectXMLElementName, propertyXMLNodeSpec);
        // System.out.println("added simple value XML element " +
        // innerObjectXMLElementName + " to " + valueDesriptionForException);
      }
      m_aJsonProperties.put (propertyName, innerObject);
    }

    if (countTextContentProperties > 1)
    {
      throw new JSONSchemaLoadException ("Object contains more then one property flaged as XML text content'" +
                                         valueDesriptionForException +
                                         "'");
    }
    if (countTextContentProperties == 1)
      this.setXMLTextContentParent (true);
  }

  public List <String> getNonWrappedJSONArrayNames ()
  {
    return m_aNonWrappedJSONArrayNames;
  }

  public PropertyXMLNodeSpec getChildElementPropertyXMLNodeSpecByName (final String name)
  {
    return m_aXmlElements.get (name);
  }

  public PropertyXMLNodeSpec getAttributePropertyXMLNodeSpecByName (final String attributeName)
  {
    return m_aXmlAttributes.get (attributeName);
  }

  public AbstractXMLNodeSpec getJSONPropertyByName (final String attributeName)
  {
    return m_aJsonProperties.get (attributeName);
  }

  void addPropertyXMLNodeSpec (final PropertyXMLNodeSpec propertyXMLNodeSpec)
  {
    final AbstractXMLNodeSpec xmlNodeSpec = propertyXMLNodeSpec.getXmlNodeSpec ();
    final String innerObjectXMLElementName = xmlNodeSpec.calculateTargetFullXMLName (propertyXMLNodeSpec.getPropertyName ());
    if (xmlNodeSpec.isPrimitiveType () && ((XMLNodeSpecPrimitiveType) xmlNodeSpec).isXMLAttribute ())
      m_aXmlAttributes.put (innerObjectXMLElementName, propertyXMLNodeSpec);
    else
    {
      m_aXmlElements.put (innerObjectXMLElementName, propertyXMLNodeSpec);
      if (xmlNodeSpec.getNodeType () == AbstractXMLNodeSpec.TYPE_ARRAY && !((XMLNodeSpecArray) xmlNodeSpec).isXMLWrapped ())
        m_aNonWrappedJSONArrayNames.add (innerObjectXMLElementName);
    }
    m_aJsonProperties.put (propertyXMLNodeSpec.getPropertyName (), xmlNodeSpec);
  }

  Map <String, PropertyXMLNodeSpec> getRefs ()
  {
    return m_aRefs;
  }

  Map <String, PropertyXMLNodeSpec> getXMLElements ()
  {
    return m_aXmlElements;
  }

  Map <String, PropertyXMLNodeSpec> getXMLAttributes ()
  {
    return m_aXmlAttributes;
  }

  public boolean isAdditionalProperties ()
  {
    return m_bIsAdditionalProperties;
  }

  private static void _checkKeyNotExistInHashMap (final String key,
                                                  final Map <String, PropertyXMLNodeSpec> hashMap,
                                                  final String valueDesriptionForException) throws JSONSchemaLoadException
  {
    if (hashMap == null || key == null)
      return;
    if (hashMap.containsKey (key))
      throw new JSONSchemaLoadException ("Element or property '" +
                                         key +
                                         "' is duplicated for object '" +
                                         valueDesriptionForException +
                                         "'");
  }

  public AbstractXMLNodeSpec getTextContent ()
  {

    int countTextContent = 0;
    int countAttibutes = 0;

    final Collection <AbstractXMLNodeSpec> specNodes = m_aJsonProperties.values ();
    final int countChildren = specNodes.size ();

    AbstractXMLNodeSpec textcontent = null;

    for (final AbstractXMLNodeSpec xmlNodeSpec : specNodes)
    {
      if (xmlNodeSpec.m_bIsXMLAttribute)
        countAttibutes++;
      if (xmlNodeSpec.m_bIsXMLTextContent)
      {
        textcontent = xmlNodeSpec;
        countTextContent++;
      }
    }

    if (countChildren - countAttibutes == 1 && countTextContent == 1)
      return textcontent;

    return null;
  }

  public PropertyXMLNodeSpec getChildElementPropertyXMLNodeSpecOfTypeTextContent ()
  {

    PropertyXMLNodeSpec propTextContent = null;
    final Set <Entry <String, AbstractXMLNodeSpec>> specNodes = m_aJsonProperties.entrySet ();

    for (final Entry <String, AbstractXMLNodeSpec> entry : specNodes)
    {
      if (entry.getValue ().m_bIsXMLTextContent)
      {
        propTextContent = new PropertyXMLNodeSpec (entry.getKey (), null, entry.getValue ());
        break;
      }
    }

    return propTextContent;

  }

}
