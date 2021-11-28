package com.helger.jsonxmlconvert.core;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

/**
 * @author Guy D. October 2018
 */
public class JSONSchemaForXML
{
  static final String DEFAULT_XML_ROOT_ELEMENT_NAME = "root";
  private static final String XML_INDENT_SPACES = "2";
  private static final int JSON_INDENT_SPACES = 2;

  private static final Logger LOGGER = Logger.getLogger (JSONSchemaForXML.class.getName ());

  private final long createdTimeInMs;
  private final PropertyXMLNodeSpec rootPropertyXMLNodeSpecObject;

  private final Map <String, PropertyXMLNodeSpec> definitionXMLNodeSpecHashMap = new HashMap <> ();

  public JSONSchemaForXML (final String source) throws JSONSchemaLoadException
  {
    createdTimeInMs = System.currentTimeMillis ();
    JSONObject schema = null;
    if (source == null)
      throw new JSONSchemaLoadException ("Failed to parse JSON: source is null");
    try
    {
      schema = new JSONObject (source);
    }
    catch (final JSONException e)
    {
      throw new JSONSchemaLoadException ("Failed to parse JSON: " + e);
    }
    // load main object
    rootPropertyXMLNodeSpecObject = _loadJSONElement (schema);
    // load definitions
    final JSONObject definitions = schema.optJSONObject ("definitions");
    if (definitions != null)
    {
      for (final String definitionName : definitions.keySet ())
      {
        final JSONObject definition = definitions.getJSONObject (definitionName);
        final PropertyXMLNodeSpec propertyXMLNodeSpec = _loadJSONElement (definition);
        if (propertyXMLNodeSpec.getRef () != null)
          throw new JSONSchemaLoadException ("Definition '" +
                                             definitionName +
                                             "' defined as a reference is not supported for this release");
        definitionXMLNodeSpecHashMap.put ("#/definitions/" + definitionName, propertyXMLNodeSpec);
      }
      // 1. resolve ref in definitions
      for (final String definitionName : definitionXMLNodeSpecHashMap.keySet ())
      {
        // can't be null
        final PropertyXMLNodeSpec propertyXMLNodeSpec = definitionXMLNodeSpecHashMap.get (definitionName);
        _resolveRef (propertyXMLNodeSpec);
      }
    }
    // 2. resolve main object
    _resolveRef (rootPropertyXMLNodeSpecObject);
    // 3. check root element if of type object
    if (rootPropertyXMLNodeSpecObject.getXmlNodeSpec ().getNodeType () != AbstractXMLNodeSpec.TYPE_OBJECT)
      throw new JSONSchemaLoadException ("Root object must be of type 'object'");
  }

  private PropertyXMLNodeSpec _getDefinitionXMLNodeSpecHashMap (final String ref) throws JSONSchemaLoadException
  {
    final PropertyXMLNodeSpec innerPropertyXMLNodeSpec = definitionXMLNodeSpecHashMap.get (ref);
    if (innerPropertyXMLNodeSpec != null)
      return innerPropertyXMLNodeSpec;
    throw new JSONSchemaLoadException ("Definition '" + ref + "' is not defined");
  }

  private void _resolveRef (final AbstractXMLNodeSpec xmlNodeSpec) throws JSONSchemaLoadException
  {
    if (xmlNodeSpec.getNodeType () == AbstractXMLNodeSpec.TYPE_OBJECT)
    {
      final XMLNodeSpecObject xmlNodeSpecObject = (XMLNodeSpecObject) xmlNodeSpec;
      final Iterator <String> propertyRefIterator = xmlNodeSpecObject.getRefs ().keySet ().iterator ();
      while (propertyRefIterator.hasNext ())
      {
        final String propertyRef = propertyRefIterator.next ();
        final PropertyXMLNodeSpec keyPropertyXMLNodeSpec = xmlNodeSpecObject.getRefs ().get (propertyRef);
        propertyRefIterator.remove ();
        _resolveRef (keyPropertyXMLNodeSpec);
        xmlNodeSpecObject.addPropertyXMLNodeSpec (keyPropertyXMLNodeSpec);
      }
      for (final PropertyXMLNodeSpec propertyXMLNodeSpec : xmlNodeSpecObject.getXMLAttributes ().values ())
        _resolveRef (propertyXMLNodeSpec);
      for (final PropertyXMLNodeSpec propertyXMLNodeSpec : xmlNodeSpecObject.getXMLElements ().values ())
        _resolveRef (propertyXMLNodeSpec);
    }
    else
      if (xmlNodeSpec.getNodeType () == AbstractXMLNodeSpec.TYPE_ARRAY)
      {
        final XMLNodeSpecArray xmlNodeSpecArray = (XMLNodeSpecArray) xmlNodeSpec;
        final String itemsRef = xmlNodeSpecArray.getItemsRef ();
        if (itemsRef != null)
        {
          if (xmlNodeSpecArray.getItemsXMLNodeSpec () == null)
          {
            final PropertyXMLNodeSpec itemsPropertyXMLNodeSpec = _getDefinitionXMLNodeSpecHashMap (itemsRef);
            xmlNodeSpecArray.setItemsXMLNodeSpec (itemsPropertyXMLNodeSpec.getXmlNodeSpec ());
            _resolveRef (itemsPropertyXMLNodeSpec);
          }
          return;
        }
        _resolveRef (xmlNodeSpecArray.getItemsXMLNodeSpec ());
      }
  }

  private void _resolveRef (final PropertyXMLNodeSpec propertyXMLNodeSpec) throws JSONSchemaLoadException
  {
    final String ref = propertyXMLNodeSpec.getRef ();
    if (ref != null)
    {
      if (propertyXMLNodeSpec.getXmlNodeSpec () == null)
      {
        final PropertyXMLNodeSpec innerPropertyXMLNodeSpec = _getDefinitionXMLNodeSpecHashMap (ref);
        propertyXMLNodeSpec.setPropertyXMLNodeSpec (innerPropertyXMLNodeSpec.getXmlNodeSpec ());
        _resolveRef (innerPropertyXMLNodeSpec);
      }
      return;
    }
    _resolveRef (propertyXMLNodeSpec.getXmlNodeSpec ());
  }

  private PropertyXMLNodeSpec _loadJSONElement (final JSONObject schema) throws JSONSchemaLoadException
  {
    try
    {
      // check if ref
      final String ref = schema.optString ("$ref", null);
      if (ref != null)
        return new PropertyXMLNodeSpec (DEFAULT_XML_ROOT_ELEMENT_NAME, ref, null);
      final String type = schema.optString ("type", null);
      final int rootNodeType = AbstractXMLNodeSpec.typeStringToTYPE (type);
      AbstractXMLNodeSpec rootXMLNodeSpec = null;
      if (rootNodeType == AbstractXMLNodeSpec.TYPE_OBJECT)
      {
        final XMLNodeSpecObject rootXMLNodeSpecObject = new XMLNodeSpecObject ();
        rootXMLNodeSpecObject.loadJSONValue (schema, "$");
        rootXMLNodeSpec = rootXMLNodeSpecObject;
      }
      else
        if (rootNodeType == AbstractXMLNodeSpec.TYPE_ARRAY)
        {
          final XMLNodeSpecArray rootXMLNodeSpecArray = new XMLNodeSpecArray ();
          rootXMLNodeSpecArray.loadJSONValue (schema, "$");
          rootXMLNodeSpec = rootXMLNodeSpecArray;
        }
        else
        {
          final XMLNodeSpecPrimitiveType rootXMLNodeSpecPrimitiveType = new XMLNodeSpecPrimitiveType (rootNodeType);
          rootXMLNodeSpecPrimitiveType.loadJSONValue (schema, "$");
          rootXMLNodeSpec = rootXMLNodeSpecPrimitiveType;
        }
      return new PropertyXMLNodeSpec (DEFAULT_XML_ROOT_ELEMENT_NAME, null, rootXMLNodeSpec);
    }
    catch (final JSONException e)
    {
      throw new JSONSchemaLoadException ("Failed to parse JSON schema: " + e);
    }
  }

  public JSONObject mapXMLToJSONObject (final String inputXML) throws MapException
  {
    final XMLToJSONConverter converter = new XMLToJSONConverter (inputXML,
                                                                 (XMLNodeSpecObject) rootPropertyXMLNodeSpecObject.getXmlNodeSpec ());
    converter.process ();
    return converter.getOutput ();
  }

  public String mapXMLToJSONString (final String inputXML, final boolean formatOutput) throws MapException
  {
    final XMLToJSONConverter converter = new XMLToJSONConverter (inputXML,
                                                                 (XMLNodeSpecObject) rootPropertyXMLNodeSpecObject.getXmlNodeSpec ());
    converter.process ();
    return jsonObjectToString (converter.getOutput (), formatOutput);
  }

  public Document mapJSONToXMLDocument (final String inputJSON) throws MapException
  {
    final JSONToXMLConverter converter = new JSONToXMLConverter (inputJSON,
                                                                 (XMLNodeSpecObject) rootPropertyXMLNodeSpecObject.getXmlNodeSpec ());
    converter.process ();
    return converter.getOutput ();
  }

  public String mapJSONToXMLString (final String inputJSON, final boolean formatOutput) throws MapException
  {
    final JSONToXMLConverter converter = new JSONToXMLConverter (inputJSON,
                                                                 (XMLNodeSpecObject) rootPropertyXMLNodeSpecObject.getXmlNodeSpec ());
    converter.process ();
    return xmlDocumentToString (converter.getOutput (), formatOutput);
  }

  public static String jsonObjectToString (final JSONObject o, final boolean formatOutput)
  {
    return o.toString (formatOutput ? JSON_INDENT_SPACES : 0);
  }

  public static String xmlDocumentToString (final Document inputDocument, final boolean bFormatOutput) throws MapException
  {
    try
    {
      final TransformerFactory transformerFactory = TransformerFactory.newInstance ();
      final Transformer transformer = transformerFactory.newTransformer ();
      transformer.setOutputProperty (OutputKeys.INDENT, bFormatOutput ? "yes" : "no");
      transformer.setOutputProperty ("{http://xml.apache.org/xslt}indent-amount", XML_INDENT_SPACES);
      transformer.setOutputProperty (OutputKeys.ENCODING, StandardCharsets.UTF_8.name ());
      // TODO fix: new line does not show at the end
      transformer.setOutputProperty (OutputKeys.OMIT_XML_DECLARATION, bFormatOutput ? "no" : "yes");
      final DOMSource domSource = new DOMSource (inputDocument);
      try (final StringWriter sw = new StringWriter ())
      {
        final StreamResult sr = new StreamResult (sw);
        transformer.transform (domSource, sr);
        return sw.toString ();
      }
    }
    catch (final Exception e)
    {
      // should never happen
      LOGGER.log (Level.WARNING, "Failed to convert XML object to a string", e);
      throw new MapException ("Failed to convert XML object to a string", e);
    }
  }

  public long getCreatedTimeInMs ()
  {
    return createdTimeInMs;
  }
}
