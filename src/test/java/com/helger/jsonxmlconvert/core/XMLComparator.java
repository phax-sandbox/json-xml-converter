package com.helger.jsonxmlconvert.core;

import org.w3c.dom.Document;

public class XMLComparator
{
  public static String getAsString (final Document d) throws MapException
  {
    return JSONSchemaForXML.xmlDocumentToString (d, false);
  }

  @Deprecated
  public static boolean areObjectsEqual (final Document d, final String xml) throws MapException
  {
    return getAsString (d).equals (xml);
  }

  public static void printAscii (final String s)
  {
    final int l = s.length ();
    int i = 0;
    while (i < l)
    {
      System.out.print (" " + (int) s.charAt (i));
      i++;
    }
    System.out.println ();
  }
}
