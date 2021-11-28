package com.helger.jsonxmlconvert;

import java.util.HashMap;
import java.util.Map;

import com.helger.jsonxmlconvert.core.JSONSchemaForXML;
import com.helger.jsonxmlconvert.core.JSONSchemaLoadException;
import com.helger.jsonxmlconvert.core.MapException;

public class JsonXmlConverter
{
  private static final Map <String, JSONSchemaForXML> MAP = new HashMap <> ();

  private JsonXmlConverter ()
  {}

  public static void registerJSONSchema (final String alias, final String jsonSchema) throws JSONSchemaLoadException
  {
    final JSONSchemaForXML jsonSchemaForXML = new JSONSchemaForXML (jsonSchema);
    MAP.put (alias, jsonSchemaForXML);
  }

  public static String mapJSONToXMLString (final String alias, final String jsonDocument) throws JSONSchemaLoadException, MapException
  {
    final JSONSchemaForXML jsonSchemaForXML = _getJSONSchema (alias);
    // true means formatted
    return jsonSchemaForXML.mapJSONToXMLString (jsonDocument, true);
  }

  public static String mapXMLToJSONString (final String alias, final String jsonDocument) throws JSONSchemaLoadException, MapException
  {
    final JSONSchemaForXML jsonSchemaForXML = _getJSONSchema (alias);
    // true means formatted
    return jsonSchemaForXML.mapXMLToJSONString (jsonDocument, true);
  }

  private static JSONSchemaForXML _getJSONSchema (final String alias) throws JSONSchemaLoadException
  {
    final JSONSchemaForXML jsonSchemaForXML = MAP.get (alias);
    if (jsonSchemaForXML == null)
      throw new JSONSchemaLoadException ("No JSON schema with alias '" + alias + "' is registered");
    return jsonSchemaForXML;
  }
}
