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

package fr.paris.lutece.plugins.pwa.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.plugins.pwa.service.ManifestBuilder;
import fr.paris.lutece.portal.service.datastore.DatastoreService;
import fr.paris.lutece.portal.service.template.AppTemplateService;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.portal.web.l10n.LocaleService;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Serves the offline page precached by the service worker. The page links the stylesheets of the site theme, which the service worker serves
 * from its asset cache, and carries its own minimal styles for when they are not cached yet
 */
public class OfflineServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;
    private static final String TEMPLATE_OFFLINE = "/skin/plugins/pwa/offline_fallback.html";
    private static final String CONTENT_TYPE = "text/html;charset=UTF-8";
    private static final String MARK_NAME = "app_name";
    private static final String MARK_BACKGROUND_COLOR = "background_color";
    private static final String MARK_THEME_COLOR = "theme_color";
    private static final String MARK_LANG = "lang";
    private static final String MARK_BASE_URL = "base_url";
    private static final String DEFAULT_BACKGROUND_COLOR = "#ffffff";
    private static final String DEFAULT_THEME_COLOR = "#000000";

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        Locale locale = LocaleService.getContextUserLocale( request );
        Map<String, Object> model = new HashMap<>( );
        model.put( MARK_NAME, StringUtils.defaultString( DatastoreService.getDataValue( ManifestBuilder.KEY_NAME, null ) ) );
        model.put( MARK_BACKGROUND_COLOR, color( ManifestBuilder.KEY_BACKGROUND_COLOR, DEFAULT_BACKGROUND_COLOR ) );
        model.put( MARK_THEME_COLOR, color( ManifestBuilder.KEY_THEME_COLOR, DEFAULT_THEME_COLOR ) );
        model.put( MARK_LANG, locale.getLanguage( ) );
        model.put( MARK_BASE_URL, AppPathService.getBaseUrl( request ) );

        response.setContentType( CONTENT_TYPE );
        response.getWriter( ).write( AppTemplateService.getTemplate( TEMPLATE_OFFLINE, locale, model ).getHtml( ) );
    }

    /**
     * Gives a color of the site properties, or the default one when it is not a hexadecimal CSS color (the value is written in a style element)
     *
     * @param strKey
     *            the site property key
     * @param strDefault
     *            the default color
     * @return the color
     */
    private static String color( String strKey, String strDefault )
    {
        return ManifestBuilder.toCssColor( DatastoreService.getDataValue( strKey, null ), strDefault );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServletInfo( )
    {
        return "This servlet delivers the offline page of the progressive web app";
    }
}
