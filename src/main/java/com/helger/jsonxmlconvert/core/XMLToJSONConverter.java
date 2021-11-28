package com.helger.jsonxmlconvert.core;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class XMLToJSONConverter
{

  private final String m_sInputXML;
  private final XMLNodeSpecObject m_aRootContainerXMLNodeSpec;
  private JSONObject m_aOutput;

  public XMLToJSONConverter (final String inputXML, final XMLNodeSpecObject rootContainerXMLNodeSpec) throws MapException
  {
    if (inputXML == null)
      throw new MapException ("Input XML is null");
    if (rootContainerXMLNodeSpec == null)
      throw new MapException ("rootXMLNodeSpec is null");
    this.m_sInputXML = inputXML;
    this.m_aRootContainerXMLNodeSpec = rootContainerXMLNodeSpec;
  }

  public void process () throws MapException
  {
    final XMLInputFactory factory = XMLInputFactory.newInstance ();
    XMLStreamReader reader = null;
    try
    {
      reader = factory.createXMLStreamReader (new StringReader (m_sInputXML));
    }
    catch (final XMLStreamException e)
    {
      throw new MapException ("XML parsing error");
    }
    // reader carries root element
    _moveReaderToFirstStartElement (reader);
    // ensure root name is correct
    final String fqElementName = _calculateFQElementName (reader);
    final String expectedFQELementName = m_aRootContainerXMLNodeSpec.calculateTargetFullXMLName (JSONSchemaForXML.DEFAULT_XML_ROOT_ELEMENT_NAME);
    final SimplePath xpath = new SimplePath (SimplePath.XML_PATH);
    xpath.pushElement (fqElementName);
    if (!fqElementName.equals (expectedFQELementName))
      throw new MapException ("XML root node name is invalid", xpath);
    m_aOutput = _parseJSONObject (reader, m_aRootContainerXMLNodeSpec, xpath);
  }

  public JSONObject getOutput ()
  {
    return m_aOutput;
  }

  static private void _moveReaderToFirstStartElement (final XMLStreamReader reader) throws MapException
  {
    try
    {
      while (true)
      {
        if (!reader.hasNext ())
          break;
        final int eventType = reader.next ();
        if (eventType == XMLStreamConstants.CHARACTERS)
        {
          if (reader.isWhiteSpace ())
          {
            // System.out.println(xpath + " CHARACTERS white spaces, ignore");
            continue;
          }
          throw new MapException ("Characters at this location are not allowed", new SimplePath (SimplePath.XML_PATH));
        }
        else
          if (eventType == XMLStreamConstants.END_ELEMENT)
          {
            // System.out.println(xpath + " END_ELEMENT: " +
            // reader.getLocalName());
            throw new MapException ("End element not allowed", new SimplePath (SimplePath.XML_PATH));
          }
          else
            if (eventType == XMLStreamConstants.START_ELEMENT)
            {
              // System.out.println(xpath + " START_ELEMENT: " +
              // reader.getLocalName());
              break;
            }
            else
              if (eventType == XMLStreamConstants.END_DOCUMENT)
              {
                break;
              }
              else
                if (eventType == XMLStreamConstants.SPACE)
                {
                  continue;
                }
                else
                  if (eventType == XMLStreamConstants.COMMENT)
                  {
                    continue;
                  }
                  else
                  {
                    throw new MapException ("Unknown StAX event type '" + eventType + "' while parsing XML",
                                            new SimplePath (SimplePath.XML_PATH));
                  }
      }
    }
    catch (final XMLStreamException e)
    {
      throw new MapException ("XML parsing error", new SimplePath (SimplePath.XML_PATH));
    }
  }

  static private Object _parseJSONValue (final XMLStreamReader reader,
                                         final AbstractXMLNodeSpec xmlNodeSpec,
                                         final SimplePath xpath,
                                         final String fqElementName) throws MapException
  {
    if (xmlNodeSpec.getNodeType () == AbstractXMLNodeSpec.TYPE_OBJECT)
      return _parseJSONObject (reader, (XMLNodeSpecObject) xmlNodeSpec, xpath);

    if (xmlNodeSpec.getNodeType () == AbstractXMLNodeSpec.TYPE_ARRAY)
    {
      final XMLNodeSpecArray xmlNodeSpecArray = (XMLNodeSpecArray) xmlNodeSpec;
      if (!xmlNodeSpecArray.isXMLWrapped ())
        return _parseJSONValue (reader, xmlNodeSpecArray.getItemsXMLNodeSpec (), xpath, fqElementName);
      return _parseJSONArrayWrapped (reader, xmlNodeSpecArray, xpath, fqElementName);
    }

    return _parseJSONPrimitiveType (reader, (XMLNodeSpecPrimitiveType) xmlNodeSpec, xpath);
  }

  static private JSONArray _parseJSONArrayWrapped (final XMLStreamReader reader,
                                                   final XMLNodeSpecArray xmlNodeSpecArray,
                                                   final SimplePath xpath,
                                                   final String wrapperFQElementName) throws MapException
  {
    final JSONArray array = new JSONArray ();
    try
    {
      while (true)
      {
        if (!reader.hasNext ())
          break;
        final int eventType = reader.next ();
        if (eventType == XMLStreamConstants.CHARACTERS)
        {
          if (reader.isWhiteSpace ())
          {
            // System.out.println(xpath + " CHARACTERS white spaces, ignore");
            continue;
          }
          throw new MapException ("Characters at this location are not allowed", xpath);
        }
        else
          if (eventType == XMLStreamConstants.SPACE)
          {
            continue;
          }
          else
            if (eventType == XMLStreamConstants.END_ELEMENT)
            {
              // System.out.println(xpath + " END_ELEMENT: " +
              // reader.getLocalName());
              break;
            }
            else
              if (eventType == XMLStreamConstants.START_ELEMENT)
              {
                // System.out.println(xpath + " START_ELEMENT: " +
                // reader.getLocalName());
                final String fqElementName = _calculateFQElementName (reader);
                xpath.pushElement (fqElementName);
                // check fqElementName is correct
                final AbstractXMLNodeSpec itemsXMLNodeSpec = xmlNodeSpecArray.getItemsXMLNodeSpec ();
                final String expectedFQElementName = itemsXMLNodeSpec.calculateTargetFullXMLName (wrapperFQElementName);
                if (!fqElementName.equals (expectedFQElementName))
                  throw new MapException ("Wrapped element name is not matching specifications", xpath);
                if (itemsXMLNodeSpec.getXmlPrefix () != null && itemsXMLNodeSpec.getXmlNamespace () != null)
                  if (!itemsXMLNodeSpec.getXmlNamespace ().equals (reader.getNamespaceURI ()))
                    throw new MapException ("Invalid namespace", xpath);
                xpath.pushIndex (array.length () + 1);
                final Object childElement = _parseJSONValue (reader, itemsXMLNodeSpec, xpath, fqElementName);
                array.put (childElement);
                xpath.pop ().pop ();
              }
              else
                if (eventType == XMLStreamConstants.END_DOCUMENT)
                {
                  throw new MapException ("Too early end of XML document", xpath);
                }
                else
                  if (eventType == XMLStreamConstants.COMMENT)
                  {
                    continue;
                  }
                  else
                  {
                    throw new MapException ("Unknown StAX event type '" + eventType + "' while parsing XML", xpath);
                  }
      }
      return array;
    }
    catch (final JSONException e)
    {
      e.printStackTrace ();
      throw new MapException ("Problem to create the target JSON object", xpath);
    }
    catch (final XMLStreamException e)
    {
      throw new MapException ("XML parsing error", xpath);
    }
  }

  static private Object _parseJSONPrimitiveType (final XMLStreamReader reader,
                                                 final XMLNodeSpecPrimitiveType xmlNodeSpecPrimitiveType,
                                                 final SimplePath xpath) throws MapException
  {
    String characters = ""; // Purposely not use StringBuilder as concatenation
                            // for this variable happens
    // rarely
    try
    {
      while (true)
      {
        if (!reader.hasNext ())
          break;
        final int eventType = reader.next ();
        if (eventType == XMLStreamConstants.CHARACTERS)
        {
          if (characters.length () == 0 && reader.isWhiteSpace ())
          {
            // System.out.println(xpath + " CHARACTERS white spaces, ignore");
            continue;
          }
          characters += reader.getText ();
        }
        else
          if (eventType == XMLStreamConstants.SPACE)
          {
            continue;
          }
          else
            if (eventType == XMLStreamConstants.END_ELEMENT)
            {
              // System.out.println(xpath + " END_ELEMENT: " +
              // reader.getLocalName());
              break;
            }
            else
              if (eventType == XMLStreamConstants.START_ELEMENT)
              {
                // System.out.println(xpath + " START_ELEMENT: " +
                // reader.getLocalName());
                xpath.pushElement (reader.getLocalName ());
                throw new MapException ("New XML element not allowed for a primitive type property (string, boolean, integer, number, null)",
                                        xpath);
              }
              else
                if (eventType == XMLStreamConstants.END_DOCUMENT)
                {
                  throw new MapException ("Too early end of XML document", xpath);
                }
                else
                  if (eventType == XMLStreamConstants.COMMENT)
                  {
                    continue;
                  }
                  else
                  {
                    throw new MapException ("Unknown StAX event type '" + eventType + "' while parsing XML", xpath);
                  }
      }
      return _createObjectFromPrimitiveTypeProperty (characters, xmlNodeSpecPrimitiveType, xpath);
    }
    catch (final JSONException e)
    {
      e.printStackTrace ();
      throw new MapException ("Problem to create the target JSON object", xpath);
    }
    catch (final XMLStreamException e)
    {
      throw new MapException ("XML parsing error", xpath);
    }
  }

  static private JSONObject _parseJSONObject (final XMLStreamReader reader,
                                              final XMLNodeSpecObject xmlNodeSpecObject,
                                              final SimplePath xpath) throws MapException
  {
    final JSONObject o = new JSONObject ();
    // add non wrapped arrays. When a wrapped array comes in, then the json
    // array is
    // added
    for (final String jsonArrayName : xmlNodeSpecObject.getNonWrappedJSONArrayNames ())
    {
      o.put (jsonArrayName, new JSONArray ());
      // System.out.println("--- proactive add array: " + jsonArrayName + " for
      // xpath:
      // " + xpath);
    }
    try
    {
      // handle attributes
      final int attributeCount = reader.getAttributeCount ();
      for (int attributeIndex = 0; attributeIndex < attributeCount; attributeIndex++)
      {
        final String prefixName = reader.getAttributePrefix (attributeIndex);
        String fqAttributeName = reader.getAttributeLocalName (attributeIndex);
        if (prefixName != null && prefixName.length () > 0)
          fqAttributeName = prefixName + ":" + fqAttributeName;
        xpath.pushXMLAttribute (fqAttributeName);
        // now attributeName is prefix:localName
        final PropertyXMLNodeSpec attributePropertyXMLNodeSpec = xmlNodeSpecObject.getAttributePropertyXMLNodeSpecByName (fqAttributeName);
        if (attributePropertyXMLNodeSpec == null)
        {
          if (!xmlNodeSpecObject.isAdditionalProperties ())
            throw new MapException ("Attribute is not defined in JSON schema and additionalProperties is set to false", xpath);
          o.accumulate (fqAttributeName, reader.getAttributeValue (attributeIndex).trim ());
          xpath.pop ();
          continue;
        }
        if (!(attributePropertyXMLNodeSpec.getXmlNodeSpec ().isPrimitiveType ()))
          throw new MapException ("Attribute should be a primitive type (string, boolean, integer, number, null)", xpath);
        if (prefixName != null && attributePropertyXMLNodeSpec.getXmlNodeSpec ().getXmlNamespace () != null)
          if (!attributePropertyXMLNodeSpec.getXmlNodeSpec ().getXmlNamespace ().equals (reader.getAttributeNamespace (attributeIndex)))
            throw new MapException ("Invalid namespace", xpath);
        final Object attributeValue = _createObjectFromPrimitiveTypeProperty (reader.getAttributeValue (attributeIndex),
                                                                              attributePropertyXMLNodeSpec.getXmlNodeSpec (),
                                                                              xpath);
        _addKeyValueToJSONObject (attributePropertyXMLNodeSpec.getPropertyName (), attributeValue, o, xpath, false);
        xpath.pop ();
      }

      boolean isCollectingTextContentLines = false;
      StringBuilder textcontent = null;

      while (true)
      {
        if (!reader.hasNext ())
          break;
        final int eventType = reader.next ();

        if (eventType != XMLStreamConstants.CHARACTERS && isCollectingTextContentLines)
        {
          xpath.pushElement ("text()");
          final PropertyXMLNodeSpec textcontentPropertyXMLNodeSpec = xmlNodeSpecObject.getChildElementPropertyXMLNodeSpecOfTypeTextContent ();

          final Object attributeValue = _createObjectFromPrimitiveTypeProperty (textcontent.toString (),
                                                                                textcontentPropertyXMLNodeSpec.getXmlNodeSpec (),
                                                                                xpath);
          _addKeyValueToJSONObject (textcontentPropertyXMLNodeSpec.getPropertyName (), attributeValue, o, xpath, false);
          xpath.pop ();

          isCollectingTextContentLines = false;
        }

        if (eventType == XMLStreamConstants.CHARACTERS)
        {
          if (reader.isWhiteSpace ())
          {
            // System.out.println(xpath + " CHARACTERS white spaces, ignore");
            continue;
          }

          // create text content property
          if (xmlNodeSpecObject.isXMLTextContentParent ())
          {
            if (!isCollectingTextContentLines)
            {
              isCollectingTextContentLines = true;
              textcontent = new StringBuilder ();
            }
            textcontent.append (reader.getText ());
            continue;
          }

          throw new MapException ("Characters at this location are not allowed", xpath);
        }
        else
          if (eventType == XMLStreamConstants.SPACE)
          {
            continue;
          }
          else
            if (eventType == XMLStreamConstants.END_ELEMENT)
            {
              // System.out.println(xpath + " END_ELEMENT: " +
              // reader.getLocalName());
              break;
            }
            else
              if (eventType == XMLStreamConstants.START_ELEMENT)
              {
                final String fqElementName = _calculateFQElementName (reader);
                xpath.pushElement (fqElementName);
                // System.out.println(xpath + " START_ELEMENT: " +
                // fqElementName);
                final PropertyXMLNodeSpec childElementPropertyXMLNodeSpec = xmlNodeSpecObject.getChildElementPropertyXMLNodeSpecByName (fqElementName);
                if (childElementPropertyXMLNodeSpec == null)
                {
                  if (!xmlNodeSpecObject.isAdditionalProperties ())
                    throw new MapException ("Element is not defined in JSON schema and additionalProperties is set to false", xpath);
                  final Object propertyValue = _parsePropertyValueWithoutSchema (reader, fqElementName, xpath);
                  o.accumulate (fqElementName, propertyValue);
                  xpath.pop ();
                  continue;
                }
                // adjust startElementXPath in case of a non wrapped array
                final boolean isAppendKeyToArray = (childElementPropertyXMLNodeSpec.getXmlNodeSpec ()
                                                                                   .getNodeType () == AbstractXMLNodeSpec.TYPE_ARRAY ? !((XMLNodeSpecArray) childElementPropertyXMLNodeSpec.getXmlNodeSpec ()).isXMLWrapped ()
                                                                                                                                     : false);
                if (isAppendKeyToArray)
                  // startElementXPath += "[" +
                  // (o.getJSONArray(childElementPropertyXMLNodeSpec.getPropertyName()).length()
                  // +
                  // 1 )+ "]";
                  xpath.pushIndex (o.getJSONArray (childElementPropertyXMLNodeSpec.getPropertyName ()).length () + 1);
                // check namespace is correct

                if (childElementPropertyXMLNodeSpec.getXmlNodeSpec ().getXmlPrefix () != null &&
                    childElementPropertyXMLNodeSpec.getXmlNodeSpec ().getXmlNamespace () != null)
                {
                  if (!childElementPropertyXMLNodeSpec.getXmlNodeSpec ().getXmlNamespace ().equals (reader.getNamespaceURI ()))
                    throw new MapException ("Invalid namespace", xpath);
                }
                // add child element
                final Object childElement = _parseJSONValue (reader,
                                                             childElementPropertyXMLNodeSpec.getXmlNodeSpec (),
                                                             xpath,
                                                             fqElementName);
                _addKeyValueToJSONObject (childElementPropertyXMLNodeSpec.getPropertyName (), childElement, o, xpath, isAppendKeyToArray);
                if (isAppendKeyToArray)
                  xpath.pop ();
                xpath.pop ();
              }
              else
                if (eventType == XMLStreamConstants.END_DOCUMENT)
                {
                  throw new MapException ("Too early end of XML document", xpath);
                }
                else
                  if (eventType == XMLStreamConstants.COMMENT)
                  {
                    continue;
                  }
                  else
                  {
                    throw new MapException ("Unknown StAX event type '" + eventType + "' while parsing XML", xpath);
                  }
      }
      return o;
    }
    catch (final JSONException e)
    {
      e.printStackTrace ();
      throw new MapException ("Problem to create the target JSON object", xpath);
    }
    catch (final XMLStreamException e)
    {
      throw new MapException ("XML parsing error", xpath);
    }
  }

  private static boolean _createTextContentProperty (final JSONObject o,
                                                     final XMLNodeSpecObject xmlNodeSpecObject,
                                                     final char [] textCharacters,
                                                     final SimplePath xpath) throws MapException
  {

    final PropertyXMLNodeSpec childElementPropertyXMLNodeSpec = xmlNodeSpecObject.getChildElementPropertyXMLNodeSpecOfTypeTextContent ();

    if (childElementPropertyXMLNodeSpec != null)
    {
      _addKeyValueToJSONObject (childElementPropertyXMLNodeSpec.getPropertyName (), textCharacters, o, xpath, false);
      return true;
    }

    return false;
  }

  static private String _calculateFQElementName (final XMLStreamReader reader)
  {
    // fully qualified (prefix:name, or name if no prefix)
    String fqElementName = reader.getLocalName ();
    // add prefix is defined
    final String prefix = reader.getPrefix ();
    if (prefix != null && prefix.length () > 0)
      fqElementName = prefix + ":" + fqElementName;
    return fqElementName;
  }

  static private void _addKeyValueToJSONObject (final String key,
                                                final Object value,
                                                final JSONObject o,
                                                final SimplePath xpath,
                                                final boolean isAppendKeyToArray) throws MapException
  {
    if (isAppendKeyToArray)
    {
      o.append (key, value);
      return;
    }
    // check no duplicate key first
    if (o.opt (key) != null)
      throw new MapException ("Duplicated element", xpath);
    // then add
    o.put (key, value);
  }

  static private Object _createObjectFromPrimitiveTypeProperty (final String characters,
                                                                final AbstractXMLNodeSpec xmlNodeSpec,
                                                                final SimplePath xpath) throws MapException
  {
    final int nodeType = xmlNodeSpec.getNodeType ();
    final String charactersTrimmed = characters.trim ();
    if (nodeType == AbstractXMLNodeSpec.TYPE_STRING)
    {
      return charactersTrimmed;
    }

    if (characters.length () == 0)
    {
      if (!xmlNodeSpec.isNullable ())
        throw new MapException ("Property is not nullable", xpath);
      return JSONObject.NULL;
    }

    if (nodeType == AbstractXMLNodeSpec.TYPE_BOOLEAN)
    {
      if (charactersTrimmed.equalsIgnoreCase ("true"))
        return Boolean.TRUE;
      else
        if (charactersTrimmed.equalsIgnoreCase ("false"))
          return Boolean.FALSE;
        else
          throw new MapException ("Failure to convert text to a boolean", xpath);
    }

    if (nodeType == AbstractXMLNodeSpec.TYPE_INTEGER)
    {
      try
      {
        if (charactersTrimmed.length () > 9)
          // we use this trick to guess it could be a very large integer
          // (int max value is
          // 10 chars)
          // we keep Integer case for performance optimization
          return new BigInteger (charactersTrimmed);
        return Integer.valueOf (Integer.parseInt (charactersTrimmed));
      }
      catch (final NumberFormatException e)
      {
        throw new MapException ("Failure to convert text to an integer", xpath, e);
      }
    }

    if (nodeType == AbstractXMLNodeSpec.TYPE_NUMBER)
    {
      try
      {
        return new BigDecimal (charactersTrimmed);
      }
      catch (final NumberFormatException e)
      {
        throw new MapException ("Failure to convert text to a number", xpath, e);
      }
    }

    // never happens
    throw new MapException ("Unknown XMLNodeSpec type " + nodeType, xpath);
  }

  static private Object _parsePropertyValueWithoutSchema (final XMLStreamReader reader,
                                                          final String propertyName,
                                                          final SimplePath xpath) throws MapException
  {
    // Purposely not use StringBuilder as concatenation for this variable
    // happens
    String characters = "";
    // rarely
    JSONObject o = null;
    try
    {
      while (true)
      {
        if (!reader.hasNext ())
          break;
        final int eventType = reader.next ();
        if (eventType == XMLStreamConstants.CHARACTERS)
        {
          if (characters.length () == 0 && reader.isWhiteSpace ())
          {
            // System.out.println(xpath + " CHARACTERS white spaces, ignore");
            continue;
          }
          if (o != null)
            throw new MapException ("Mix XML elements and text is not allowed", xpath);
          characters += reader.getText ();
        }
        else
          if (eventType == XMLStreamConstants.SPACE)
          {
            continue;
          }
          else
            if (eventType == XMLStreamConstants.END_ELEMENT)
            {
              // System.out.println(xpath + " END_ELEMENT: " +
              // reader.getLocalName());
              break;
            }
            else
              if (eventType == XMLStreamConstants.START_ELEMENT)
              {
                // System.out.println(xpath + " START_ELEMENT: " +
                // reader.getLocalName());
                final String fqElementName = _calculateFQElementName (reader);
                xpath.pushElement (fqElementName);
                if (characters.length () != 0)
                  throw new MapException ("Mix XML elements and text is not allowed", xpath);
                final int attributeCount = reader.getAttributeCount ();
                for (int attributeIndex = 0; attributeIndex < attributeCount; attributeIndex++)
                {
                  final String prefixName = reader.getAttributePrefix (attributeIndex);
                  String fqAttributeName = reader.getAttributeLocalName (attributeIndex);
                  if (prefixName != null && prefixName.length () > 0)
                    fqAttributeName = prefixName + ":" + fqAttributeName;
                  o.accumulate (fqAttributeName, reader.getAttributeValue (attributeIndex).trim ());
                }
                final Object innerObject = _parsePropertyValueWithoutSchema (reader, fqElementName, xpath);
                if (o == null)
                  o = new JSONObject ();
                o.accumulate (fqElementName, innerObject);
                xpath.pop ();
              }
              else
                if (eventType == XMLStreamConstants.END_DOCUMENT)
                {
                  throw new MapException ("Too early end of XML document", xpath);
                }
                else
                  if (eventType == XMLStreamConstants.COMMENT)
                  {
                    continue;
                  }
                  else
                  {
                    throw new MapException ("Unknown StAX event type '" + eventType + "' while parsing XML", xpath);
                  }
      }
      if (o != null)
        return o;
      return characters.trim ();
    }
    catch (final JSONException e)
    {
      throw new MapException ("Problem to create the target JSON object", xpath, e);
    }
    catch (final XMLStreamException e)
    {
      throw new MapException ("XML parsing error", xpath, e);
    }
  }
}
