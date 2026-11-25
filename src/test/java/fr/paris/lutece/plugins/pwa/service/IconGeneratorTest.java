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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

/**
 * Tests the generation of the icons
 */
public class IconGeneratorTest
{
    /**
     * Builds a test image: a red disc on a transparent background
     *
     * @param nWidth
     *            the width
     * @param nHeight
     *            the height
     * @param strFormat
     *            png or jpeg
     * @return the encoded image
     * @throws IOException
     *             if the image cannot be written
     */
    static byte [ ] image( int nWidth, int nHeight, String strFormat ) throws IOException
    {
        int nType = IconGenerator.FORMAT_PNG.equals( strFormat ) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage image = new BufferedImage( nWidth, nHeight, nType );
        Graphics2D graphics = image.createGraphics( );
        graphics.setColor( Color.RED );
        int nSide = Math.min( nWidth, nHeight );
        graphics.fillOval( ( nWidth - nSide ) / 2, ( nHeight - nSide ) / 2, nSide, nSide );
        graphics.dispose( );

        ByteArrayOutputStream output = new ByteArrayOutputStream( );
        ImageIO.write( image, strFormat, output );

        return output.toByteArray( );
    }

    /**
     * A PNG and a JPEG are recognized, any other content is not
     *
     * @throws IOException
     *             if an image cannot be read
     */
    @Test
    public void testFormats( ) throws IOException
    {
        assertEquals( IconGenerator.FORMAT_PNG, IconGenerator.getFormat( image( 10, 10, "png" ) ) );
        assertEquals( IconGenerator.FORMAT_JPEG, IconGenerator.getFormat( image( 10, 10, "jpeg" ) ) );
        assertNull( IconGenerator.getFormat( "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes( ) ) );
        assertNull( IconGenerator.read( new byte [ 0 ] ) );
    }

    /**
     * A rectangular image is cropped to its centered square and scaled to the requested size, keeping its transparency
     *
     * @throws IOException
     *             if an image cannot be read or written
     */
    @Test
    public void testIcon( ) throws IOException
    {
        BufferedImage source = IconGenerator.read( image( 1200, 800, "png" ) );

        BufferedImage icon = ImageIO.read( new java.io.ByteArrayInputStream( IconGenerator.icon( source, 192 ) ) );

        assertEquals( 192, icon.getWidth( ) );
        assertEquals( 192, icon.getHeight( ) );
        assertEquals( 0, icon.getRGB( 0, 0 ) >>> 24, "the transparent corner stays transparent" );
        assertEquals( Color.RED.getRGB( ), icon.getRGB( 96, 96 ) | 0xFF000000, "the disc fills the center" );
    }

    /**
     * The maskable icon has the background color in its corners and the image inside the safe zone
     *
     * @throws IOException
     *             if an image cannot be read or written
     */
    @Test
    public void testMaskable( ) throws IOException
    {
        BufferedImage source = IconGenerator.read( image( 600, 600, "png" ) );

        BufferedImage icon = ImageIO.read( new java.io.ByteArrayInputStream( IconGenerator.maskable( source, 512, IconGenerator.parseColor( "#336699", null ) ) ) );

        assertEquals( 512, icon.getWidth( ) );
        assertEquals( new Color( 0x33, 0x66, 0x99 ).getRGB( ), icon.getRGB( 2, 2 ), "full-bleed background" );
        assertEquals( new Color( 0x33, 0x66, 0x99 ).getRGB( ), icon.getRGB( 256, 60 ), "nothing drawn outside the safe zone" );
        assertEquals( Color.RED.getRGB( ), icon.getRGB( 256, 256 ) );
    }

    /**
     * Short and long hexadecimal colors are parsed, anything else gives the default
     */
    @Test
    public void testParseColor( )
    {
        assertEquals( new Color( 0xff, 0xff, 0xff ), IconGenerator.parseColor( "#fff", null ) );
        assertEquals( new Color( 0x12, 0xab, 0x34 ), IconGenerator.parseColor( " #12AB34 ", null ) );
        assertEquals( Color.BLACK, IconGenerator.parseColor( "red", Color.BLACK ) );
        assertEquals( Color.BLACK, IconGenerator.parseColor( null, Color.BLACK ) );
    }
}
