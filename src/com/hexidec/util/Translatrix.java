/*
GNU Lesser General Public License

Translatrix - General Access To Language Resource Bundles
Copyright (C) 2002  Howard A Kistler

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package com.hexidec.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Translatrix
{
	private static ResourceBundle langResources;
	private static String bundleName;

	public Translatrix(String bundle)
	{
		bundleName = new String(bundle);
		try
		{
			langResources = ResourceBundle.getBundle(bundleName);
		}
		catch(MissingResourceException mre)
		{
			logException("MissingResourceException while loading language file", mre);
		}
	}

	public static void setBundleName(String bundle)
	{
		bundleName = new String(bundle);
		try
		{
			langResources = ResourceBundle.getBundle(bundleName);
		}
		catch(MissingResourceException mre)
		{
			logException("MissingResourceException while loading language file", mre);
		}
	}

	public static void setLocale(Locale locale)
	{
		if(bundleName == null)
		{
			return;
		}
		if(locale != null)
		{
			try
			{
				langResources = ResourceBundle.getBundle(bundleName, locale);
			}
			catch(MissingResourceException mre1)
			{
				try
				{
					langResources = ResourceBundle.getBundle(bundleName);
				}
				catch(MissingResourceException mre2)
				{
					logException("MissingResourceException while loading language file", mre2);
				}
			}
		}
		else
		{
			try
			{
				langResources = ResourceBundle.getBundle(bundleName);
			}
			catch(MissingResourceException mre)
			{
				logException("MissingResourceException while loading language file", mre);
			}
		}
	}

	public static void setLocale(String sLanguage, String sCountry)
	{
		if(sLanguage != null && sCountry != null)
		{
			setLocale(new Locale(sLanguage, sCountry));
		}
	}

	public static String getTranslationString(String originalText)
	{
		if(langResources == null || bundleName == null)
		{
			return originalText;
		}
		else
		{
			try
			{
				return langResources.getString(originalText);
			}
			catch(Exception e)
			{
				return originalText;
			}
		}
	}

	private static void logException(String internalMessage, Exception e)
	{
		System.err.println(internalMessage);
		e.printStackTrace(System.err);
	}

}