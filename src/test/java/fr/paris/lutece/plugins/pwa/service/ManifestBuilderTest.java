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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests the web app manifest builder
 */
public class ManifestBuilderTest
{
    private static final String CONTEXT_PATH = "/lutece";
    private final ObjectMapper _mapper = new ObjectMapper( );

    /**
     * Parses the manifest built from the given site properties
     *
     * @param properties
     *            the site properties
     * @return the manifest
     * @throws Exception
     *             if the manifest is not valid JSON
     */
    private JsonNode build( Map<String, String> properties ) throws Exception
    {
        return _mapper.readTree( ManifestBuilder.build( CONTEXT_PATH, "fr", properties::get ) );
    }

    /**
     * Quotes, backslashes, markup and accents entered in the site properties keep the manifest valid JSON and come back unchanged
     *
     * @throws Exception
     *             if the manifest is not valid JSON
     */
    @Test
    public void testValuesAreEscaped( ) throws Exception
    {
        Map<String, String> properties = new HashMap<>( );
        properties.put( ManifestBuilder.KEY_NAME, "Mairie \"de\" Paris \\ <b>été</b>" );
        properties.put( ManifestBuilder.KEY_DESCRIPTION, "Démarches en ligne, 24h/24\nsur mobile" );

        JsonNode manifest = build( properties );

        assertEquals( "Mairie \"de\" Paris \\ <b>été</b>", manifest.get( "name" ).asText( ) );
        assertEquals( "Mairie \"de\" Paris \\ <b>été</b>", manifest.get( "short_name" ).asText( ), "short_name falls back on name" );
        assertEquals( "Démarches en ligne, 24h/24\nsur mobile", manifest.get( "description" ).asText( ) );
    }

    /**
     * Without any site property, the manifest is still installable: a name, the root of the webapp as start URL and scope, standalone display, the 192 and
     * 512 icons under the context path
     *
     * @throws Exception
     *             if the manifest is not valid JSON
     */
    @Test
    public void testDefaults( ) throws Exception
    {
        JsonNode manifest = build( new HashMap<>( ) );

        assertFalse( manifest.get( "name" ).asText( ).isBlank( ) );
        assertEquals( "/lutece/", manifest.get( "start_url" ).asText( ) );
        assertEquals( "/lutece/", manifest.get( "scope" ).asText( ) );
        assertEquals( "standalone", manifest.get( "display" ).asText( ) );
        assertFalse( manifest.has( "orientation" ), "no orientation lock by default" );
        assertFalse( manifest.has( "description" ) );
        assertEquals( "/lutece/", manifest.get( "id" ).asText( ) );
        assertEquals( "fr", manifest.get( "lang" ).asText( ) );
        assertFalse( manifest.has( "categories" ) );
        assertEquals( 4, manifest.get( "icons" ).size( ) );
        assertEquals( "any", manifest.get( "icons" ).get( 1 ).get( "purpose" ).asText( ) );
        assertEquals( "maskable", manifest.get( "icons" ).get( 3 ).get( "purpose" ).asText( ) );
        assertEquals( "/lutece/themes/shared/plugins/pwa/images/icon-maskable-512.png", manifest.get( "icons" ).get( 3 ).get( "src" ).asText( ) );
        assertEquals( "/lutece/themes/shared/plugins/pwa/images/icon-192.png", manifest.get( "icons" ).get( 1 ).get( "src" ).asText( ) );
        assertEquals( "512x512", manifest.get( "icons" ).get( 2 ).get( "sizes" ).asText( ) );
    }

    /**
     * A relative start URL is resolved against the root of the webapp, an absolute path is kept, an unknown display or orientation falls back on the default
     *
     * @throws Exception
     *             if the manifest is not valid JSON
     */
    @Test
    public void testUrlsAndAllowedValues( ) throws Exception
    {
        Map<String, String> properties = new HashMap<>( );
        properties.put( ManifestBuilder.KEY_START_URL, "jsp/site/Portal.jsp?page=search" );
        properties.put( ManifestBuilder.KEY_SCOPE, "/" );
        properties.put( ManifestBuilder.KEY_DISPLAY, "Fullscreen " );
        properties.put( ManifestBuilder.KEY_ORIENTATION, "upside-down" );
        properties.put( ManifestBuilder.KEY_THEME_COLOR, " #0000FF " );
        properties.put( ManifestBuilder.KEY_CATEGORIES, "Government, news,,government " );

        JsonNode manifest = build( properties );

        assertEquals( "/lutece/jsp/site/Portal.jsp?page=search", manifest.get( "start_url" ).asText( ) );
        assertEquals( "/", manifest.get( "scope" ).asText( ) );
        assertEquals( "fullscreen", manifest.get( "display" ).asText( ) );
        assertFalse( manifest.has( "orientation" ), "an unknown orientation is left out" );
        assertEquals( "#0000FF", manifest.get( "theme_color" ).asText( ) );
        assertEquals( "/lutece/", manifest.get( "id" ).asText( ), "the id stays the root whatever the start URL, so the installed app keeps its identity" );
        assertEquals( "[\"government\",\"news\"]", manifest.get( "categories" ).toString( ) );
    }

    /**
     * A webapp deployed at the root has no context path
     *
     * @throws Exception
     *             if the manifest is not valid JSON
     */
    @Test
    public void testRootContext( ) throws Exception
    {
        JsonNode manifest = _mapper.readTree( ManifestBuilder.build( "", null, key -> null ) );

        assertEquals( "/", manifest.get( "start_url" ).asText( ) );
        assertFalse( manifest.has( "lang" ) );
        assertEquals( "/themes/shared/plugins/pwa/images/icon-48.png", manifest.get( "icons" ).get( 0 ).get( "src" ).asText( ) );
    }
}
