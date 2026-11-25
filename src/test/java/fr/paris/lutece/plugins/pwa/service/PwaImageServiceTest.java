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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import fr.paris.lutece.test.LuteceTestCase;
import jakarta.inject.Inject;

/**
 * Tests the storage of the icons and screenshots in the file store of the core
 */
public class PwaImageServiceTest extends LuteceTestCase
{
    @Inject
    private PwaImageService _imageService;

    /**
     * Removes what a test stored
     */
    @AfterEach
    public void cleanImages( )
    {
        _imageService.resetIcon( );
        _imageService.getScreenshots( ).forEach( image -> _imageService.removeScreenshot( image.getFileKey( ) ) );
    }

    /**
     * An uploaded image gives the three plain icons and the maskable one, served from the file store and listed in the manifest; a reset removes them
     *
     * @throws Exception
     *             if an image cannot be generated
     */
    @Test
    public void testIconLifecycle( ) throws Exception
    {
        _imageService.setIcon( IconGeneratorTest.image( 800, 800, "png" ), "#336699" );

        Map<String, PwaImage> icons = _imageService.getIcons( );
        assertEquals( List.of( "icon-48", "icon-192", "icon-512", PwaImageService.ICON_MASKABLE ), List.copyOf( icons.keySet( ) ) );
        PwaImage icon192 = icons.get( "icon-192" );
        assertEquals( 192, IconGenerator.read( _imageService.getContent( icon192 ) ).getWidth( ) );
        assertTrue( _imageService.findImage( icon192.getFileKey( ) ).isPresent( ) );

        List<Map<String, String>> listIcons = _imageService.getManifestIcons( "/ctx" );
        assertEquals( 4, listIcons.size( ) );
        assertEquals( "/ctx/servlet/plugins/pwa/image?key=" + icon192.getFileKey( ), listIcons.get( 1 ).get( "src" ) );
        assertEquals( "maskable", listIcons.get( 3 ).get( "purpose" ) );

        _imageService.resetIcon( );
        assertTrue( _imageService.getIcons( ).isEmpty( ) );
        assertNull( _imageService.getManifestIcons( "/ctx" ), "the bundled icons are used again" );
        assertFalse( _imageService.findImage( icon192.getFileKey( ) ).isPresent( ) );
    }

    /**
     * A too small icon, a screenshot out of bounds and a file that is not an image are refused with their message
     *
     * @throws Exception
     *             if an image cannot be generated
     */
    @Test
    public void testRefusedImages( ) throws Exception
    {
        assertRefused( PwaImageService.MESSAGE_ICON_TOO_SMALL, ( ) -> _imageService.setIcon( IconGeneratorTest.image( 256, 256, "png" ), null ) );
        assertRefused( PwaImageService.MESSAGE_NOT_AN_IMAGE, ( ) -> _imageService.setIcon( "not an image".getBytes( ), null ) );
        assertRefused( PwaImageService.MESSAGE_DIMENSIONS_TOO_LARGE,
                ( ) -> _imageService.setIcon( IconGeneratorTest.image( PwaImageService.MAX_IMAGE_SIZE + 1, 512, "png" ), null ) );
        assertRefused( PwaImageService.MESSAGE_SCREENSHOT_SIZE, ( ) -> _imageService.addScreenshot( IconGeneratorTest.image( 200, 400, "png" ), null ) );
        assertRefused( PwaImageService.MESSAGE_SCREENSHOT_RATIO, ( ) -> _imageService.addScreenshot( IconGeneratorTest.image( 400, 1200, "jpeg" ), null ) );
        assertTrue( _imageService.getIcons( ).isEmpty( ) );
        assertTrue( _imageService.getScreenshots( ).isEmpty( ) );
    }

    /**
     * Screenshots keep their format, get their form factor from their orientation, and are removed one by one
     *
     * @throws Exception
     *             if an image cannot be generated
     */
    @Test
    public void testScreenshots( ) throws Exception
    {
        _imageService.addScreenshot( IconGeneratorTest.image( 1280, 720, "jpeg" ), "Home page" );
        _imageService.addScreenshot( IconGeneratorTest.image( 540, 1170, "png" ), " " );

        List<Map<String, String>> listScreenshots = _imageService.getManifestScreenshots( "" );
        assertEquals( 2, listScreenshots.size( ) );
        assertEquals( "wide", listScreenshots.get( 0 ).get( "form_factor" ) );
        assertEquals( "image/jpeg", listScreenshots.get( 0 ).get( "type" ) );
        assertEquals( "Home page", listScreenshots.get( 0 ).get( "label" ) );
        assertEquals( "narrow", listScreenshots.get( 1 ).get( "form_factor" ) );
        assertFalse( listScreenshots.get( 1 ).containsKey( "label" ), "a blank label is left out" );

        String strFirstKey = _imageService.getScreenshots( ).get( 0 ).getFileKey( );
        assertTrue( _imageService.removeScreenshot( strFirstKey ) );
        assertFalse( _imageService.removeScreenshot( strFirstKey ) );
        assertEquals( 1, _imageService.getScreenshots( ).size( ) );
    }

    /**
     * Asserts that an upload is refused with the given message
     *
     * @param strMessageKey
     *            the expected message
     * @param upload
     *            the upload
     */
    private static void assertRefused( String strMessageKey, Upload upload )
    {
        PwaImageException exception = assertThrows( PwaImageException.class, upload::run );
        assertEquals( strMessageKey, exception.getMessage( ) );
    }

    /**
     * An upload that may be refused
     */
    @FunctionalInterface
    private interface Upload
    {
        /**
         * Runs the upload
         *
         * @throws Exception
         *             if it is refused
         */
        void run( ) throws Exception;
    }
}
