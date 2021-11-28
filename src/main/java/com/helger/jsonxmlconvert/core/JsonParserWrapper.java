package com.helger.jsonxmlconvert.core;

import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

/**
 * This class is a wrapper for JsonParser. It simply helps catching exceptions
 * related to this package and map it to a MapException with JSON path
 * information
 *
 * @author Guy D.
 */
public class JsonParserWrapper
{
  private final JsonParser m_aParser;

  public JsonParserWrapper (final JsonParser aParser)
  {
    m_aParser = aParser;
  }

  public boolean hasNext (final SimplePath jpath) throws MapException
  {
    try
    {
      return m_aParser.hasNext ();
    }
    catch (final Exception e)
    {
      throw new MapException ("Failed to parse JSON input", jpath, e);
    }
  }

  public Event next (final SimplePath jpath) throws MapException
  {
    try
    {
      return m_aParser.next ();
    }
    catch (final Exception e)
    {
      throw new MapException ("Failed to parse JSON input", jpath, e);
    }
  }

  public String getString (final SimplePath jpath) throws MapException
  {
    try
    {
      return m_aParser.getString ();
    }
    catch (final Exception e)
    {
      throw new MapException ("Failed to parse JSON input", jpath, e);
    }
  }

  public String getPrimitiveValueAsString (final SimplePath jpath) throws MapException
  {
    try
    {
      final JsonValue jsonValue = m_aParser.getValue ();
      if (jsonValue.getValueType () == ValueType.STRING || jsonValue.getValueType () == ValueType.NUMBER)
        return m_aParser.getString ();
      if (jsonValue.getValueType () == ValueType.FALSE)
        return "false";
      if (jsonValue.getValueType () == ValueType.TRUE)
        return "true";
      if (jsonValue.getValueType () == ValueType.NULL)
        return "";
      throw new MapException ("Invalid primitive type", jpath);
    }
    catch (final Exception e)
    {
      throw new MapException ("Failed to parse JSON input", jpath, e);
    }
  }
}
