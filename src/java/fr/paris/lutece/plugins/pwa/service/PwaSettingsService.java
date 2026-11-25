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
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.portal.service.datastore.DatastoreService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Reads and saves the settings of the progressive web app, kept in the datastore, and checks what the browsers require to install it
 */
@ApplicationScoped
public class PwaSettingsService
{
    public static final List<String> KEYS = List.of( ManifestBuilder.KEY_NAME, ManifestBuilder.KEY_SHORT_NAME, ManifestBuilder.KEY_DESCRIPTION,
            ManifestBuilder.KEY_DISPLAY, ManifestBuilder.KEY_ORIENTATION, ManifestBuilder.KEY_START_URL, ManifestBuilder.KEY_SCOPE,
            ManifestBuilder.KEY_BACKGROUND_COLOR, ManifestBuilder.KEY_THEME_COLOR, ManifestBuilder.KEY_CATEGORIES );

    private static final String CHECK_PREFIX = "pwa.manage_settings.checklist.";
    private static final String HEADER_FORWARDED_PROTO = "X-Forwarded-Proto";
    private static final String HTTPS = "https";
    private static final String DISPLAY_BROWSER = "browser";
    private static final List<String> LOCAL_HOSTS = List.of( "localhost", "127.0.0.1", "[::1]" );

    @Inject
    private PwaImageService _imageService;

    /**
     * Gives the settings
     *
     * @return the value of each setting by key, empty when not set
     */
    public Map<String, String> getSettings( )
    {
        Map<String, String> settings = new LinkedHashMap<>( );
        KEYS.forEach( strKey -> settings.put( strKey, StringUtils.defaultString( DatastoreService.getDataValue( strKey, null ) ) ) );

        return settings;
    }

    /**
     * Saves the settings: only the known keys are written
     *
     * @param settings
     *            the value of each setting by key
     */
    public void saveSettings( Map<String, String> settings )
    {
        settings.forEach( ( strKey, strValue ) -> {
            if ( KEYS.contains( strKey ) )
            {
                DatastoreService.setDataValue( strKey, StringUtils.trimToEmpty( strValue ) );
            }
        } );
    }

    /**
     * Checks what the browsers require, or recommend, to install the site as an application
     *
     * @param request
     *            the request of the administrator, to know how the site is served
     * @return the checklist
     */
    public List<ChecklistItem> getChecklist( HttpServletRequest request )
    {
        Map<String, String> settings = getSettings( );
        List<ChecklistItem> listItems = new ArrayList<>( );
        String strRoot = request.getContextPath( ) + "/";

        listItems.add( item( "https", isSecure( request ), ChecklistItem.STATUS_ERROR ) );
        listItems.add( item( "name", StringUtils.isNotBlank( settings.get( ManifestBuilder.KEY_NAME ) ), ChecklistItem.STATUS_WARNING ) );
        listItems.add( item( "display", !DISPLAY_BROWSER.equalsIgnoreCase( settings.get( ManifestBuilder.KEY_DISPLAY ).trim( ) ), ChecklistItem.STATUS_ERROR ) );

        String strStartUrl = ManifestBuilder.resolveAppUrl( settings.get( ManifestBuilder.KEY_START_URL ), strRoot );
        String strScope = ManifestBuilder.resolveAppUrl( settings.get( ManifestBuilder.KEY_SCOPE ), strRoot );
        listItems.add( item( "scope", strStartUrl.startsWith( strScope ), ChecklistItem.STATUS_ERROR ) );

        listItems.add( item( "icons", !_imageService.getIcons( ).isEmpty( ), ChecklistItem.STATUS_WARNING ) );
        listItems.add( item( "description", StringUtils.isNotBlank( settings.get( ManifestBuilder.KEY_DESCRIPTION ) ), ChecklistItem.STATUS_WARNING ) );

        List<PwaImage> listScreenshots = _imageService.getScreenshots( );
        boolean bWide = listScreenshots.stream( ).anyMatch( image -> image.getWidth( ) > image.getHeight( ) );
        boolean bNarrow = listScreenshots.stream( ).anyMatch( image -> image.getWidth( ) <= image.getHeight( ) );
        listItems.add( item( "screenshots", bWide && bNarrow, ChecklistItem.STATUS_WARNING ) );

        return listItems;
    }

    /**
     * Whether the site is served over HTTPS, directly or behind a proxy, or on the local machine where browsers allow service workers over HTTP
     *
     * @param request
     *            the request
     * @return true if the browsers will register the service worker
     */
    private static boolean isSecure( HttpServletRequest request )
    {
        return request.isSecure( ) || HTTPS.equalsIgnoreCase( StringUtils.trimToEmpty( request.getHeader( HEADER_FORWARDED_PROTO ) ) )
                || LOCAL_HOSTS.contains( StringUtils.defaultString( request.getServerName( ) ) );
    }

    /**
     * Builds an item of the checklist
     *
     * @param strCheck
     *            the name of the check
     * @param bPassed
     *            whether the check passed
     * @param strFailedStatus
     *            the status when the check failed
     * @return the item
     */
    private static ChecklistItem item( String strCheck, boolean bPassed, String strFailedStatus )
    {
        return new ChecklistItem( CHECK_PREFIX + strCheck, bPassed ? ChecklistItem.STATUS_OK : strFailedStatus );
    }
}
