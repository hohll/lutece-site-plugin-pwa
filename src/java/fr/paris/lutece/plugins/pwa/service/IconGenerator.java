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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * Reads uploaded images and generates the application icons: plain icons cropped to a square, and a maskable icon whose content stays inside the safe zone
 * (a circle of 80% of the icon) on a full-bleed background
 */
public final class IconGenerator
{
    public static final String FORMAT_PNG = "png";
    public static final String FORMAT_JPEG = "jpeg";
    public static final double MASKABLE_CONTENT_RATIO = 0.56;
    private static final int MIN_STEP_SIZE = 2;

    /**
     * Private constructor
     */
    private IconGenerator( )
    {
    }

    /**
     * Reads an image
     *
     * @param content
     *            the bytes of the image
     * @return the image, or null when the bytes are not a PNG or JPEG image
     * @throws IOException
     *             if the bytes cannot be read
     */
    public static BufferedImage read( byte [ ] content ) throws IOException
    {
        return getFormat( content ) == null ? null : ImageIO.read( new ByteArrayInputStream( content ) );
    }

    /**
     * Gives the format of an image
     *
     * @param content
     *            the bytes of the image
     * @return png or jpeg, null for any other format
     * @throws IOException
     *             if the bytes cannot be read
     */
    public static String getFormat( byte [ ] content ) throws IOException
    {
        if ( content == null || content.length == 0 )
        {
            return null;
        }

        try ( ImageInputStream stream = ImageIO.createImageInputStream( new ByteArrayInputStream( content ) ) )
        {
            Iterator<ImageReader> readers = ImageIO.getImageReaders( stream );
            if ( !readers.hasNext( ) )
            {
                return null;
            }

            String strFormat = readers.next( ).getFormatName( ).toLowerCase( Locale.ROOT );
            if ( FORMAT_PNG.equals( strFormat ) )
            {
                return FORMAT_PNG;
            }

            return "jpg".equals( strFormat ) || FORMAT_JPEG.equals( strFormat ) ? FORMAT_JPEG : null;
        }
    }

    /**
     * Gives the dimensions an image declares in its header, without decoding its pixels
     *
     * @param content
     *            the bytes of the image
     * @return the width and the height, or null when the bytes are not a readable image
     * @throws IOException
     *             if the header cannot be read
     */
    public static int [ ] getSize( byte [ ] content ) throws IOException
    {
        if ( content == null || content.length == 0 )
        {
            return null;
        }

        try ( ImageInputStream stream = ImageIO.createImageInputStream( new ByteArrayInputStream( content ) ) )
        {
            Iterator<ImageReader> readers = ImageIO.getImageReaders( stream );
            if ( !readers.hasNext( ) )
            {
                return null;
            }

            ImageReader reader = readers.next( );
            try
            {
                reader.setInput( stream, true, true );
                return new int [ ] {
                        reader.getWidth( 0 ), reader.getHeight( 0 )
                };
            }
            finally
            {
                reader.dispose( );
            }
        }
    }

    /**
     * Generates a square icon: the image is cropped to its centered square, then scaled
     *
     * @param source
     *            the source image
     * @param nSize
     *            the side of the icon in pixels
     * @return the icon as PNG
     * @throws IOException
     *             if the PNG cannot be written
     */
    public static byte [ ] icon( BufferedImage source, int nSize ) throws IOException
    {
        return toPng( scale( cropSquare( source ), nSize ) );
    }

    /**
     * Generates a maskable icon: a full-bleed background with the image, cropped to its centered square, scaled into the safe zone
     *
     * @param source
     *            the source image
     * @param nSize
     *            the side of the icon in pixels
     * @param background
     *            the background color
     * @return the icon as PNG
     * @throws IOException
     *             if the PNG cannot be written
     */
    public static byte [ ] maskable( BufferedImage source, int nSize, Color background ) throws IOException
    {
        int nContent = (int) Math.round( nSize * MASKABLE_CONTENT_RATIO );
        BufferedImage content = scale( cropSquare( source ), nContent );
        BufferedImage icon = new BufferedImage( nSize, nSize, BufferedImage.TYPE_INT_RGB );
        Graphics2D graphics = icon.createGraphics( );

        try
        {
            graphics.setColor( background );
            graphics.fillRect( 0, 0, nSize, nSize );
            int nOffset = ( nSize - nContent ) / 2;
            graphics.drawImage( content, nOffset, nOffset, null );
        }
        finally
        {
            graphics.dispose( );
        }

        return toPng( icon );
    }

    /**
     * Parses a hexadecimal CSS color (#rgb or #rrggbb)
     *
     * @param strColor
     *            the color
     * @param defaultColor
     *            the color returned when the value cannot be parsed
     * @return the color
     */
    public static Color parseColor( String strColor, Color defaultColor )
    {
        String strHex = strColor == null ? "" : strColor.trim( );

        if ( strHex.matches( "^#[0-9a-fA-F]{3}$" ) )
        {
            strHex = "#" + strHex.charAt( 1 ) + strHex.charAt( 1 ) + strHex.charAt( 2 ) + strHex.charAt( 2 ) + strHex.charAt( 3 ) + strHex.charAt( 3 );
        }

        return strHex.matches( "^#[0-9a-fA-F]{6}$" ) ? Color.decode( strHex ) : defaultColor;
    }

    /**
     * Crops an image to its centered square
     *
     * @param source
     *            the image
     * @return the square image
     */
    private static BufferedImage cropSquare( BufferedImage source )
    {
        int nSide = Math.min( source.getWidth( ), source.getHeight( ) );
        int nX = ( source.getWidth( ) - nSide ) / 2;
        int nY = ( source.getHeight( ) - nSide ) / 2;

        return source.getSubimage( nX, nY, nSide, nSide );
    }

    /**
     * Scales a square image, halving it step by step while it is more than twice the target so the result stays sharp
     *
     * @param source
     *            the square image
     * @param nSize
     *            the target side
     * @return the scaled image, with alpha
     */
    private static BufferedImage scale( BufferedImage source, int nSize )
    {
        BufferedImage current = source;
        int nCurrent = source.getWidth( );

        do
        {
            nCurrent = ( nCurrent / MIN_STEP_SIZE > nSize ) ? nCurrent / MIN_STEP_SIZE : nSize;
            current = draw( current, nCurrent );
        }
        while ( nCurrent != nSize );

        return current;
    }

    /**
     * Draws an image at a new size
     *
     * @param source
     *            the image
     * @param nSize
     *            the side of the result
     * @return the result, with alpha
     */
    private static BufferedImage draw( BufferedImage source, int nSize )
    {
        BufferedImage result = new BufferedImage( nSize, nSize, BufferedImage.TYPE_INT_ARGB );
        Graphics2D graphics = result.createGraphics( );

        try
        {
            graphics.setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC );
            graphics.setRenderingHint( RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY );
            graphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
            graphics.drawImage( source, 0, 0, nSize, nSize, null );
        }
        finally
        {
            graphics.dispose( );
        }

        return result;
    }

    /**
     * Encodes an image as PNG
     *
     * @param image
     *            the image
     * @return the PNG bytes
     * @throws IOException
     *             if the PNG cannot be written
     */
    private static byte [ ] toPng( BufferedImage image ) throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream( );
        ImageIO.write( image, FORMAT_PNG, output );

        return output.toByteArray( );
    }
}
