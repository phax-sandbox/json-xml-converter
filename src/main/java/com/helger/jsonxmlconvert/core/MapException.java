package com.helger.jsonxmlconvert.core;

/**
 * @author Guy D. October 2018
 */
public class MapException extends Exception
{
  private final SimplePath m_aPath;

  public MapException (final String message)
  {
    super (message);
    m_aPath = null;
  }

  public MapException (final String message, final Throwable aCause)
  {
    super (message, aCause);
    m_aPath = null;
  }

  public MapException (final String message, final SimplePath path)
  {
    super (message);
    m_aPath = path;
  }

  public MapException (final String message, final SimplePath path, final Throwable aCause)
  {
    super (message, aCause);
    m_aPath = path;
  }

  @Override
  public String toString ()
  {
    if (m_aPath == null)
      return super.toString ();
    return super.toString () + " (path: " + m_aPath + ")";
  }
}
