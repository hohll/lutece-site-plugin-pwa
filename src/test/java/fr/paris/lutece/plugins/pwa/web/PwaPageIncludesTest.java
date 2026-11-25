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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.pwa.service.ManifestBuilder;
import fr.paris.lutece.portal.service.content.PageData;
import fr.paris.lutece.portal.service.datastore.DatastoreService;
import fr.paris.lutece.test.LuteceTestCase;
import fr.paris.lutece.test.mocks.MockHttpServletRequest;
import freemarker.core.HTMLOutputFormat;
import freemarker.core.TemplateHTMLOutputModel;
import freemarker.template.TemplateModelException;

/**
 * Tests the page includes of the plugin
 */
public class PwaPageIncludesTest extends LuteceTestCase
{
    private String _strShortName;
    private String _strThemeColor;

    /**
     * Restores the site properties a test changed
     */
    @AfterEach
    public void restoreProperties( )
    {
        if ( _strShortName != null )
        {
            DatastoreService.setDataValue( ManifestBuilder.KEY_SHORT_NAME, _strShortName );
        }
        if ( _strThemeColor != null )
        {
            DatastoreService.setDataValue( ManifestBuilder.KEY_THEME_COLOR, _strThemeColor );
        }
    }

    /**
     * The head include renders every tag of the head, escapes the application title and leaves out a theme color that is not a CSS color
     */
    @Test
    public void testHeadInclude( ) throws TemplateModelException
    {
        _strShortName = DatastoreService.getDataValue( ManifestBuilder.KEY_SHORT_NAME, "" );
        _strThemeColor = DatastoreService.getDataValue( ManifestBuilder.KEY_THEME_COLOR, "" );
        DatastoreService.setDataValue( ManifestBuilder.KEY_SHORT_NAME, "Paris \"<script>\"" );
        DatastoreService.setDataValue( ManifestBuilder.KEY_THEME_COLOR, "red\"><script>" );

        Map<String, Object> model = new HashMap<>( );
        new PwaHeadInclude( ).fillTemplate( model, new PageData( ), 0, new MockHttpServletRequest( ) );

        String strHead = markup( model, PwaHeadInclude.MARK_HEAD );
        assertNotNull( strHead );
        assertTrue( strHead.contains( "<link rel=\"manifest\" href=\"servlet/plugins/pwa/ManifestServlet\">" ) );
        assertTrue( strHead.contains( "themes/shared/plugins/pwa/js/sw-registration.js" ) );
        assertTrue( strHead.contains( "rel=\"apple-touch-icon\"" ) );
        assertTrue( strHead.contains( "Paris &quot;&lt;script&gt;&quot;" ), "the application title is escaped" );
        assertFalse( strHead.contains( "<script>" ), "no markup of the site properties reaches the page" );
        assertFalse( strHead.contains( "name=\"theme-color\"" ), "an invalid theme color is left out" );
        assertFalse( strHead.contains( "#i18n{" ) );
    }

    /**
     * The manifest include puts the manifest link in the page model
     */
    @Test
    public void testManifestInclude( ) throws TemplateModelException
    {
        Map<String, Object> model = new HashMap<>( );
        new ManifestInclude( ).fillTemplate( model, new PageData( ), 0, new MockHttpServletRequest( ) );

        String strLink = markup( model, "pwa_manifest_link" );
        assertNotNull( strLink );
        assertTrue( strLink.contains( "servlet/plugins/pwa/ManifestServlet" ) );
        assertTrue( strLink.contains( "rel=\"manifest\"" ) );
    }

    /**
     * The service worker include puts the registration script in the page model
     */
    @Test
    public void testServiceWorkerInclude( ) throws TemplateModelException
    {
        Map<String, Object> model = new HashMap<>( );
        new ServiceWorkerInclude( ).fillTemplate( model, new PageData( ), 0, new MockHttpServletRequest( ) );

        String strScript = markup( model, "pwa_service_worker" );
        assertNotNull( strScript );
        assertTrue( strScript.contains( "themes/shared/plugins/pwa/js/sw-registration.js" ) );
    }

    /**
     * Reads a mark of the model: the includes put their HTML in as markup, so that the auto-escaping of the templates
     * prints it as is.
     *
     * @param model
     *            the model filled by the include
     * @param strMark
     *            the mark
     * @return the HTML of the mark, null when the mark is absent
     * @throws TemplateModelException
     *             if the markup cannot be read
     */
    private static String markup( Map<String, Object> model, String strMark ) throws TemplateModelException
    {
        Object value = model.get( strMark );
        assertTrue( value == null || value instanceof TemplateHTMLOutputModel, strMark + " is put in the model as HTML markup" );

        return value == null ? null : HTMLOutputFormat.INSTANCE.getMarkupString( (TemplateHTMLOutputModel) value );
    }
}
