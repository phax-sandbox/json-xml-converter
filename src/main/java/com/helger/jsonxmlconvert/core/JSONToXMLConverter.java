package com.helger.jsonxmlconvert.core;

import java.io.StringReader;

import javax.json.Json;
import javax.json.stream.JsonParser.Event;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class JSONToXMLConverter
{
  private final String m_sInputJSON;
  private final XMLNodeSpecObject m_aRootContainerXMLNodeSpec;
  private Document m_aOutput;
  private final NamespaceStack <String> m_aNamespaceStack = new NamespaceStack <> ();

  public JSONToXMLConverter (final String inputJSON, final XMLNodeSpecObject rootContainerXMLNodeSpec) throws MapException
  {
    if (inputJSON == null)
      throw new MapException ("JSON input is null");
    if (rootContainerXMLNodeSpec == null)
      throw new MapException ("rootXMLNodeSpec is null");
    m_sInputJSON = inputJSON;
    m_aRootContainerXMLNodeSpec = rootContainerXMLNodeSpec;
  }

  public void process () throws MapException
  {
    // System.out.println("***********");
    final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance ();
    DocumentBuilder docBuilder;
    try
    {
      docBuilder = docFactory.newDocumentBuilder ();
    }
    catch (final ParserConfigurationException e1)
    {
      // should never happen
      throw new MapException ("Internal problem while instanciating DocumentBuilder: " + e1);
    }
    m_aOutput = docBuilder.newDocument ();
    m_aOutput.setXmlStandalone (true);
    final SimplePath jpath = new SimplePath (SimplePath.JSON_PATH);

    m_aNamespaceStack.push (m_aRootContainerXMLNodeSpec.getXmlNamespace ());
    final String currentNamespace = m_aNamespaceStack.peek ();

    final Element rootElement = _createObjectElement (m_aOutput,
                                                      m_aRootContainerXMLNodeSpec,
                                                      JSONSchemaForXML.DEFAULT_XML_ROOT_ELEMENT_NAME,
                                                      jpath,
                                                      currentNamespace);
    m_aOutput.appendChild (rootElement);
    JsonParserWrapper parser = null;
    try
    {
      parser = new JsonParserWrapper (Json.createParser (new StringReader (m_sInputJSON)));
    }
    catch (final Exception ex)
    {
      throw new MapException ("Failed to parse JSON input", jpath);
    }
    final Event e = parser.next (jpath);
    if (e != Event.START_OBJECT)
      throw new MapException ("JSON input type must an object, starting with {", jpath);
    _parseObject (parser, rootElement, m_aRootContainerXMLNodeSpec, "", jpath);

    // Assert to be empty after pop. Just to be consistent.
    m_aNamespaceStack.pop ();
  }

  public Document getOutput ()
  {
    return m_aOutput;
  }

  private void _parseArray (final JsonParserWrapper parser,
                           final Element objectElement,
                           final XMLNodeSpecArray xmlNodeSpecArray,
                           final String currentPropertyName,
                           final SimplePath jpath) throws MapException
  {
    Element targetElement = objectElement; // current objectElement is array is
                                           // not wrapped or new element to be
    // created bellow
    String arrayFQElementName = currentPropertyName;
    if (xmlNodeSpecArray.isXMLWrapped ())
    {

      m_aNamespaceStack.push (xmlNodeSpecArray.getXmlNamespace ());
      final String wrapperNamespace = m_aNamespaceStack.peek ();

      final String targetFQELementName = xmlNodeSpecArray.calculateArrayTargetFullXMLName (currentPropertyName);

      // TODO: Testcases
      final Element newArrayElement = _createObjectElement (objectElement, xmlNodeSpecArray, targetFQELementName, jpath, wrapperNamespace);
      arrayFQElementName = newArrayElement.getNodeName ();
      objectElement.appendChild (newArrayElement);
      targetElement = newArrayElement;
    }
    int arrayIndex = 0;
    while (parser.hasNext (jpath))
    {
      final Event e = parser.next (jpath);
      if (e == Event.START_ARRAY)
      {
        jpath.pushIndex (arrayIndex++);

        m_aNamespaceStack.push (xmlNodeSpecArray.getItemsXMLNodeSpec ().getXmlNamespace ());

        if (!(xmlNodeSpecArray.getItemsXMLNodeSpec () instanceof XMLNodeSpecArray))
          throw new MapException ("Value should not be a JSON array", jpath);
        _parseArray (parser, targetElement, (XMLNodeSpecArray) xmlNodeSpecArray.getItemsXMLNodeSpec (), arrayFQElementName, jpath);

        m_aNamespaceStack.pop ();
        jpath.pop ();
      }
      else
        if (e == Event.END_ARRAY)
        {
          if (xmlNodeSpecArray.isXMLWrapped ())
          {
            m_aNamespaceStack.pop ();
          }
          return;
        }
        else
          if (e == Event.START_OBJECT)
          {
            jpath.pushIndex (arrayIndex++);
            if (!(xmlNodeSpecArray.getItemsXMLNodeSpec () instanceof XMLNodeSpecObject))
              throw new MapException ("Value should not be a JSON object", jpath);

            m_aNamespaceStack.push (xmlNodeSpecArray.getItemsXMLNodeSpec ().getXmlNamespace ());
            final String currentNamespace = m_aNamespaceStack.peek ();

            final Element childObjectElement = _createObjectElement (targetElement,
                                                                     xmlNodeSpecArray.getItemsXMLNodeSpec (),
                                                                     arrayFQElementName,
                                                                     jpath,
                                                                     currentNamespace);
            targetElement.appendChild (childObjectElement);
            _parseObject (parser,
                         childObjectElement,
                         (XMLNodeSpecObject) xmlNodeSpecArray.getItemsXMLNodeSpec (),
                         arrayFQElementName,
                         jpath);
            m_aNamespaceStack.pop ();
            jpath.pop ();
          }
          else
            if (e == Event.VALUE_STRING ||
                e == Event.VALUE_NUMBER ||
                e == Event.VALUE_NULL ||
                e == Event.VALUE_TRUE ||
                e == Event.VALUE_FALSE)
            {
              jpath.pushIndex (arrayIndex++);
              final String memberValue = _getAndValidateValue (parser, e, xmlNodeSpecArray.getItemsXMLNodeSpec (), jpath);
              _addKeyValueLeafElement (targetElement,
                                       (XMLNodeSpecPrimitiveType) xmlNodeSpecArray.getItemsXMLNodeSpec (),
                                       arrayFQElementName,
                                       memberValue,
                                       jpath);
              jpath.pop ();
            }
            else
              throw new MapException ("Invalid event type", jpath);
    }
  }

  private void _parseObject (final JsonParserWrapper parser,
                            final Element objectElement,
                            final XMLNodeSpecObject xmlNodeSpecObject,
                            final String currentPropertyName,
                            final SimplePath jpath) throws MapException
  {
    String memberKey = null;
    AbstractXMLNodeSpec childXmlNodeSpec = null;

    while (parser.hasNext (jpath))
    {
      final Event e = parser.next (jpath);
      if (e == Event.START_ARRAY)
      {
        if (!(childXmlNodeSpec instanceof XMLNodeSpecArray))
          throw new MapException ("Value should not be a JSON array", jpath);
        _parseArray (parser, objectElement, (XMLNodeSpecArray) childXmlNodeSpec, memberKey, jpath);
        jpath.pop ();
      }
      else
        if (e == Event.START_OBJECT)
        {
          if (!(childXmlNodeSpec instanceof XMLNodeSpecObject))
            throw new MapException ("Value should not be a JSON object", jpath);

          m_aNamespaceStack.push (childXmlNodeSpec.getXmlNamespace ());
          final String currentNamespace = m_aNamespaceStack.peek ();

          final Element childObjectElement = _createObjectElement (objectElement, childXmlNodeSpec, memberKey, jpath, currentNamespace);
          // System.out.println(childObjectElement.getTextContent());
          objectElement.appendChild (childObjectElement);
          _parseObject (parser, childObjectElement, (XMLNodeSpecObject) childXmlNodeSpec, memberKey, jpath);
          m_aNamespaceStack.pop ();
          jpath.pop ();
        }
        else
          if (e == Event.END_OBJECT)
          {

            // Debug
            // System.out.println(objectElement.getTextContent());

            return;
          }
          else
            if (e == Event.KEY_NAME)
            {
              memberKey = parser.getString (jpath);
              jpath.pushElement (memberKey);
              childXmlNodeSpec = xmlNodeSpecObject.getJSONPropertyByName (memberKey);
              if (childXmlNodeSpec == null)
              {
                if (xmlNodeSpecObject.isAdditionalProperties ())
                {
                  _addAdditionalProperty (parser, objectElement, memberKey, jpath);
                }
                else
                  throw new MapException ("Property is not defined in JSON schema and additionalProperties is false", jpath);
              }
            }
            else
              if (e == Event.VALUE_STRING ||
                  e == Event.VALUE_TRUE ||
                  e == Event.VALUE_FALSE ||
                  e == Event.VALUE_NUMBER ||
                  e == Event.VALUE_NULL)
              {
                final String memberValue = _getAndValidateValue (parser, e, childXmlNodeSpec, jpath);
                _addKeyValueLeafElement (objectElement, (XMLNodeSpecPrimitiveType) childXmlNodeSpec, memberKey, memberValue, jpath);
                jpath.pop ();
              }
              else
                throw new MapException ("Invalid event type", jpath);
    }

  }

  private String _getAndValidateValue (final JsonParserWrapper parser,
                                      final Event currentEvent,
                                      final AbstractXMLNodeSpec xmlNodeSpec,
                                      final SimplePath jpath) throws MapException
  {
    final int nodeType = xmlNodeSpec.getNodeType ();
    if (currentEvent == Event.VALUE_STRING)
    {
      if (nodeType != AbstractXMLNodeSpec.TYPE_STRING)
        throw new MapException ("Value should not be a string", jpath);
      return parser.getString (jpath);
    }
    else
      if (currentEvent == Event.VALUE_NUMBER)
      {
        if (nodeType != AbstractXMLNodeSpec.TYPE_NUMBER && nodeType != AbstractXMLNodeSpec.TYPE_INTEGER)
          throw new MapException ("Value should not be a number or an integer", jpath);
        return parser.getString (jpath);
      }
      else
        if (currentEvent == Event.VALUE_TRUE)
        {
          if (nodeType != AbstractXMLNodeSpec.TYPE_BOOLEAN)
            throw new MapException ("Value should not be a boolean", jpath);
          return "true";
        }
        else
          if (currentEvent == Event.VALUE_FALSE)
          {
            if (nodeType != AbstractXMLNodeSpec.TYPE_BOOLEAN)
              throw new MapException ("Value should not be a boolean", jpath);
            return "false";
          }
          else
            if (currentEvent == Event.VALUE_NULL)
            {
              // check if null object is allowed
              if (!xmlNodeSpec.isNullable ())
                throw new MapException ("Property is not nullable", jpath);
              return "";
            }
    throw new MapException ("Internal error", jpath);
  }

  private void _addKeyValueLeafElement (final Element objectElement,
                                        final XMLNodeSpecPrimitiveType propertyXMLNodeSpec,
                                        final String objectKey,
                                        final String objectValue,
                                        final SimplePath jpath) throws MapException
  {
    // System.out.println("addKeyValueLeafElement objectKey=" + objectKey + ",
    // objectElement.getNodeName()="
    // + objectElement.getNodeName());
    final String targetFQELementName = propertyXMLNodeSpec.calculateTargetFullXMLName (objectKey);
    if (propertyXMLNodeSpec.isXMLAttribute ())
    {
      // System.out.println("is attribute targetFQELementName:" +
      // targetFQELementName);
      objectElement.setAttribute (targetFQELementName, objectValue);
      return;
    }

    // TODO: Testfälle
    // Text Content wird gesetzt
    if (propertyXMLNodeSpec.isXMLTextContent ())
    {
      objectElement.setTextContent (objectValue);
      return;
    }

    m_aNamespaceStack.push (propertyXMLNodeSpec.getXmlNamespace ());
    final String currentNamespace = m_aNamespaceStack.peek ();

    final Element childElement = _createObjectElement (objectElement, propertyXMLNodeSpec, objectKey, jpath, currentNamespace);
    objectElement.appendChild (childElement);
    childElement.appendChild (objectElement.getOwnerDocument ().createTextNode (objectValue));
    m_aNamespaceStack.pop ();
  }

  private Element _createObjectElement (final Node node,
                                        final AbstractXMLNodeSpec xmlNodeSpec,
                                        final String currentPropertyName,
                                        final SimplePath jpath,
                                        final String currentNamespace)
  {
    final String targetFQELementName = xmlNodeSpec.calculateTargetFullXMLName (currentPropertyName);

    final Element element;
    if (node instanceof Document)
    {
      // root element are handled different
      if (currentNamespace != null)
        element = ((Document) node).createElementNS (currentNamespace, targetFQELementName);
      else
        element = ((Document) node).createElement (targetFQELementName);
    }
    else
    {
      if (currentNamespace != null)
        element = node.getOwnerDocument ().createElementNS (currentNamespace, targetFQELementName);
      else
        element = node.getOwnerDocument ().createElement (targetFQELementName);
    }

    return element;

  }

  private void _addAdditionalProperty (final JsonParserWrapper parser,
                                       final Element objectElement,
                                       final String memberKey,
                                       final SimplePath jpath) throws MapException
  {
    final Event e = parser.next (jpath);
    if (e == Event.START_ARRAY)
    {
      // level 1 array
      _addArrayWithoutSchema (parser, objectElement, memberKey, jpath);
      jpath.pop (); // previous index of array
    }
    else
      if (e == Event.START_OBJECT)
      {
        final Element propertyElement = objectElement.getOwnerDocument ().createElement (memberKey);
        objectElement.appendChild (propertyElement);
        _addObjectWithoutSchema (parser, propertyElement, memberKey, jpath);
      }
      else
        if (e == Event.VALUE_STRING || e == Event.VALUE_TRUE || e == Event.VALUE_FALSE || e == Event.VALUE_NUMBER)
        {
          final Element propertyElement = objectElement.getOwnerDocument ().createElement (memberKey);
          objectElement.appendChild (propertyElement);
          propertyElement.appendChild (propertyElement.getOwnerDocument ().createTextNode (parser.getPrimitiveValueAsString (jpath)));
        }
        else
          if (e == Event.VALUE_NULL)
            throw new MapException ("Additonal property is not nullable", jpath);
          else
            throw new MapException ("Invalid property value structure", jpath);
    jpath.pop ();
  }

  private void _addArrayWithoutSchema (final JsonParserWrapper parser,
                                       final Element propertyElement,
                                       final String propertyName,
                                       final SimplePath jpath) throws MapException
  {
    int arrayIndex = 0;
    jpath.pushIndex (arrayIndex);
    while (parser.hasNext (jpath))
    {
      final Event e = parser.next (jpath);
      if (e == Event.START_ARRAY)
      {
        // level 2 or more array
        final Element childPropertyElement = propertyElement.getOwnerDocument ().createElement (propertyName);
        propertyElement.appendChild (childPropertyElement);
        _addArrayWithoutSchema (parser, childPropertyElement, propertyName, jpath); // may
                                                                                    // use
                                                                                    // "item"
                                                                                    // instead
                                                                                    // of
        // propertyName
        jpath.pop (); // previous index of previous array
        jpath.pop (); // previous index of array
        jpath.pushIndex (++arrayIndex);
      }
      else
        if (e == Event.START_OBJECT)
        {
          final Element childPropertyElement = propertyElement.getOwnerDocument ().createElement (propertyName);
          propertyElement.appendChild (childPropertyElement);
          _addObjectWithoutSchema (parser, childPropertyElement, propertyName, jpath);
          jpath.pop (); // previous index of array
          jpath.pushIndex (++arrayIndex);
        }
        else
          if (e == Event.END_ARRAY)
          {
            return;
          }
          else
            if (e == Event.VALUE_STRING || e == Event.VALUE_TRUE || e == Event.VALUE_FALSE || e == Event.VALUE_NUMBER)
            {
              final Element childPropertyElement = propertyElement.getOwnerDocument ().createElement (propertyName);
              propertyElement.appendChild (childPropertyElement);
              childPropertyElement.appendChild (propertyElement.getOwnerDocument ()
                                                               .createTextNode (parser.getPrimitiveValueAsString (jpath)));
              jpath.pop (); // previous index of array
              jpath.pushIndex (++arrayIndex);
            }
            else
              if (e == Event.VALUE_NULL)
                throw new MapException ("Additonal property is not nullable", jpath);
              else
                throw new MapException ("Invalid event type", jpath);
    }
  }

  private void _addObjectWithoutSchema (final JsonParserWrapper parser,
                                       final Element propertyElement,
                                       final String propertyName,
                                       final SimplePath jpath) throws MapException
  {
    String memberKey = null;
    while (parser.hasNext (jpath))
    {
      final Event e = parser.next (jpath);
      if (e == Event.START_ARRAY)
      {
        // level 1 array
        _addArrayWithoutSchema (parser, propertyElement, memberKey, jpath);
        jpath.pop (); // previous index of array
        jpath.pop (); // previous memberKey
      }
      else
        if (e == Event.START_OBJECT)
        {
          final Element childPropertyElement = propertyElement.getOwnerDocument ().createElement (memberKey);
          propertyElement.appendChild (childPropertyElement);
          _addObjectWithoutSchema (parser, childPropertyElement, memberKey, jpath);
          jpath.pop (); // previous memberKey
        }
        else
          if (e == Event.END_OBJECT)
          {
            return;
          }
          else
            if (e == Event.KEY_NAME)
            {
              memberKey = parser.getString (jpath);
              jpath.pushElement (memberKey);
            }
            else
              if (e == Event.VALUE_STRING || e == Event.VALUE_TRUE || e == Event.VALUE_FALSE || e == Event.VALUE_NUMBER)
              {
                final Element childPropertyElement = propertyElement.getOwnerDocument ().createElement (memberKey);
                propertyElement.appendChild (childPropertyElement);
                childPropertyElement.appendChild (childPropertyElement.getOwnerDocument ()
                                                                      .createTextNode (parser.getPrimitiveValueAsString (jpath)));
                jpath.pop (); // previous memberKey
              }
              else
                if (e == Event.VALUE_NULL)
                  throw new MapException ("Additonal property is not nullable", jpath);
                else
                  throw new MapException ("Invalid event type", jpath);
    }
  }
}
