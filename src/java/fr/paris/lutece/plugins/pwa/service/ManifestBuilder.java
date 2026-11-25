/*
 * Copyright (c) 2002-2026, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */

package fr.paris.lutece.plugins.pwa.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Builds the web app manifest from the PWA site properties. The JSON is serialized by Jackson, so every value is escaped whatever the site enters.
 */
public final class ManifestBuilder
{
    public static final String KEY_NAME = "pwa.site_property.01.name";
    public static final String KEY_SHORT_NAME = "pwa.site_property.02.short_name";
    public static final String KEY_DESCRIPTION = "pwa.site_property.03.description";
    public static final String KEY_DISPLAY = "pwa.site_property.04.display";
    public static final String KEY_ORIENTATION = "pwa.site_property.05.orientation";
    public static final String KEY_START_URL = "pwa.site_property.06.start.url";
    public static final String KEY_SCOPE = "pwa.site_property.07.scope";
    public static final String KEY_BACKGROUND_COLOR = "pwa.site_property.10.background.color";
    public static final String KEY_THEME_COLOR = "pwa.site_property.11.theme.color";
    public static final String KEY_CATEGORIES = "pwa.site_property.12.categories";

    public static final String DEFAULT_DISPLAY = "standalone";
    public static final String ICONS_PATH = "/themes/shared/plugins/pwa/images/icon-";
    public static final int [ ] ICON_SIZES = {
            48, 192, 512
    };
    public static final String MASKABLE_ICON_PATH = "/themes/shared/plugins/pwa/images/icon-maskable-512.png";

    public static final List<String> DISPLAY_VALUES = List.of( DEFAULT_DISPLAY, "fullscreen", "minimal-ui", "browser" );
    public static final List<String> ORIENTATION_VALUES = List.of( "any", "natural", "portrait", "portrait-primary", "portrait-secondary", "landscape",
            "landscape-primary", "landscape-secondary" );
    private static final String SEPARATOR = "/";
    private static final String ICON_EXTENSION = ".png";
    private static final String ICON_TYPE = "image/png";
    private static final String DEFAULT_NAME = "Lutece";
    private static final String MASKABLE_SIZES = "512x512";
    private static final String PURPOSE_ANY = "any";
    private static final String PURPOSE_MASKABLE = "maskable";
    private static final String CATEGORY_SEPARATOR = ",";
    private static final String COLOR_PATTERN = "^#[0-9a-fA-F]{3,8}$";
    private static final ObjectMapper MAPPER = new ObjectMapper( );

    /**
     * Private constructor
     */
    private ManifestBuilder( )
    {
    }

    /**
     * Builds the manifest
     *
     * @param strContextPath
     *            the context path of the webapp ("" when it is deployed at the root)
     * @param strLanguage
     *            the language of the site (BCP 47 tag), left out of the manifest when blank
     * @param properties
     *            gives the value of a site property key, null or blank when it is not set
     * @return the manifest as JSON
     */
    public static String build( String strContextPath, String strLanguage, UnaryOperator<String> properties )
    {
        return build( strContextPath, strLanguage, properties, null, null );
    }

    /**
     * Builds the manifest with the uploaded icons and screenshots
     *
     * @param strContextPath
     *            the context path of the webapp ("" when it is deployed at the root)
     * @param strLanguage
     *            the language of the site (BCP 47 tag), left out of the manifest when blank
     * @param properties
     *            gives the value of a site property key, null or blank when it is not set
     * @param listIcons
     *            the uploaded icons, null to use the icons bundled with the plugin
     * @param listScreenshots
     *            the screenshots, null or empty when there is none
     * @return the manifest as JSON
     */
    public static String build( String strContextPath, String strLanguage, UnaryOperator<String> properties, List<Map<String, String>> listIcons,
            List<Map<String, String>> listScreenshots )
    {
        String strRoot = StringUtils.defaultString( strContextPath ) + SEPARATOR;
        Map<String, Object> manifest = new LinkedHashMap<>( );
        String strStartUrl = resolveAppUrl( properties.apply( KEY_START_URL ), strRoot );

        manifest.put( "id", strRoot );
        String strName = StringUtils.defaultIfBlank( properties.apply( KEY_NAME ), DEFAULT_NAME ).trim( );
        manifest.put( "name", strName );
        manifest.put( "short_name", StringUtils.defaultIfBlank( properties.apply( KEY_SHORT_NAME ), strName ).trim( ) );
        putIfNotBlank( manifest, "description", properties.apply( KEY_DESCRIPTION ) );
        putIfNotBlank( manifest, "lang", strLanguage );
        manifest.put( "start_url", strStartUrl );
        manifest.put( "scope", resolveAppUrl( properties.apply( KEY_SCOPE ), strRoot ) );
        manifest.put( "display", valueIn( properties.apply( KEY_DISPLAY ), DISPLAY_VALUES, DEFAULT_DISPLAY ) );
        putIfNotBlank( manifest, "orientation", valueIn( properties.apply( KEY_ORIENTATION ), ORIENTATION_VALUES, null ) );
        putIfNotBlank( manifest, "background_color", properties.apply( KEY_BACKGROUND_COLOR ) );
        putIfNotBlank( manifest, "theme_color", properties.apply( KEY_THEME_COLOR ) );
        List<String> listCategories = buildCategories( properties.apply( KEY_CATEGORIES ) );
        if ( !listCategories.isEmpty( ) )
        {
            manifest.put( "categories", listCategories );
        }
        manifest.put( "icons", listIcons != null ? listIcons : buildIcons( strContextPath ) );
        if ( listScreenshots != null && !listScreenshots.isEmpty( ) )
        {
            manifest.put( "screenshots", listScreenshots );
        }

        try
        {
            return MAPPER.writerWithDefaultPrettyPrinter( ).writeValueAsString( manifest );
        }
        catch( JsonProcessingException e )
        {
            throw new IllegalStateException( "Unable to serialize the web app manifest", e );
        }
    }

    /**
     * Gives a color entered in the site properties when it is a hexadecimal CSS color, the default value otherwise. Use it wherever the color is written in
     * markup or styles.
     *
     * @param strValue
     *            the value entered in the site properties
     * @param strDefault
     *            the default value
     * @return the color
     */
    public static String toCssColor( String strValue, String strDefault )
    {
        String strColor = StringUtils.trimToEmpty( strValue );

        return strColor.matches( COLOR_PATTERN ) ? strColor : strDefault;
    }

    /**
     * Builds the icons of the manifest
     *
     * @param strContextPath
     *            the context path of the webapp
     * @return the icons
     */
    private static List<Map<String, String>> buildIcons( String strContextPath )
    {
        List<Map<String, String>> listIcons = new ArrayList<>( );

        for ( int nSize : ICON_SIZES )
        {
            Map<String, String> icon = new LinkedHashMap<>( );
            icon.put( "src", StringUtils.defaultString( strContextPath ) + ICONS_PATH + nSize + ICON_EXTENSION );
            icon.put( "sizes", nSize + "x" + nSize );
            icon.put( "type", ICON_TYPE );
            icon.put( "purpose", PURPOSE_ANY );
            listIcons.add( icon );
        }

        Map<String, String> maskable = new LinkedHashMap<>( );
        maskable.put( "src", StringUtils.defaultString( strContextPath ) + MASKABLE_ICON_PATH );
        maskable.put( "sizes", MASKABLE_SIZES );
        maskable.put( "type", ICON_TYPE );
        maskable.put( "purpose", PURPOSE_MASKABLE );
        listIcons.add( maskable );

        return listIcons;
    }

    /**
     * Builds the categories of the manifest from a comma separated list
     *
     * @param strCategories
     *            the categories entered in the site properties
     * @return the categories, lower case, without blanks nor duplicates
     */
    private static List<String> buildCategories( String strCategories )
    {
        List<String> listCategories = new ArrayList<>( );

        for ( String strCategory : StringUtils.split( StringUtils.defaultString( strCategories ), CATEGORY_SEPARATOR ) )
        {
            String strNormalized = strCategory.trim( ).toLowerCase( Locale.ROOT );
            if ( !strNormalized.isEmpty( ) && !listCategories.contains( strNormalized ) )
            {
                listCategories.add( strNormalized );
            }
        }

        return listCategories;
    }

    /**
     * Resolves a URL of the application: blank gives the root of the webapp, a relative path is resolved against the root of the webapp, an absolute path or
     * URL is kept
     *
     * @param strValue
     *            the value entered in the site properties
     * @param strRoot
     *            the root of the webapp, ending with a slash
     * @return the URL
     */
    public static String resolveAppUrl( String strValue, String strRoot )
    {
        if ( StringUtils.isBlank( strValue ) )
        {
            return strRoot;
        }

        String strUrl = strValue.trim( );

        if ( strUrl.startsWith( SEPARATOR ) || strUrl.contains( "://" ) )
        {
            return strUrl;
        }

        return strRoot + strUrl;
    }

    /**
     * Gives the value when it belongs to the allowed values, the default value otherwise
     *
     * @param strValue
     *            the value entered in the site properties
     * @param setAllowed
     *            the allowed values
     * @param strDefault
     *            the default value
     * @return the value to use
     */
    private static String valueIn( String strValue, List<String> setAllowed, String strDefault )
    {
        String strNormalized = StringUtils.trimToEmpty( strValue ).toLowerCase( Locale.ROOT );

        return setAllowed.contains( strNormalized ) ? strNormalized : strDefault;
    }

    /**
     * Puts a trimmed value in the manifest when it is not blank
     *
     * @param manifest
     *            the manifest
     * @param strMember
     *            the member name
     * @param strValue
     *            the value
     */
    private static void putIfNotBlank( Map<String, Object> manifest, String strMember, String strValue )
    {
        if ( StringUtils.isNotBlank( strValue ) )
        {
            manifest.put( strMember, strValue.trim( ) );
        }
    }
}
