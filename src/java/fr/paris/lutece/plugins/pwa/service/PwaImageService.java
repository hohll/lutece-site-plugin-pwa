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
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.paris.lutece.portal.service.datastore.DatastoreService;
import fr.paris.lutece.portal.service.file.FileService;
import fr.paris.lutece.portal.service.file.FileServiceException;
import fr.paris.lutece.portal.service.file.IFileStoreServiceProvider;
import fr.paris.lutece.portal.service.util.AppLogService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Manages the images of the progressive web app: the icons generated from one uploaded image, and the screenshots shown by the install dialog. The files are
 * kept in the file store of the core, their description in the datastore.
 */
@ApplicationScoped
public class PwaImageService
{
    public static final String ICON_MASKABLE = "icon-maskable-512";
    public static final int MIN_ICON_SIZE = 512;
    public static final int MIN_SCREENSHOT_SIZE = 320;
    public static final int MAX_SCREENSHOT_SIZE = 3840;
    public static final double MAX_SCREENSHOT_RATIO = 2.3;
    public static final int MAX_SCREENSHOTS = 8;
    public static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;
    public static final int MAX_IMAGE_SIZE = 8192;

    public static final String MESSAGE_NOT_AN_IMAGE = "pwa.message.image.notAnImage";
    public static final String MESSAGE_TOO_LARGE = "pwa.message.image.tooLarge";
    public static final String MESSAGE_DIMENSIONS_TOO_LARGE = "pwa.message.image.dimensionsTooLarge";
    public static final String MESSAGE_ICON_TOO_SMALL = "pwa.message.icon.tooSmall";
    public static final String MESSAGE_SCREENSHOT_SIZE = "pwa.message.screenshot.size";
    public static final String MESSAGE_SCREENSHOT_RATIO = "pwa.message.screenshot.ratio";
    public static final String MESSAGE_SCREENSHOT_LIMIT = "pwa.message.screenshot.limit";
    public static final String MESSAGE_STORE_FAILED = "pwa.message.image.storeFailed";

    private static final String KEY_ICONS = "pwa.images.icons";
    private static final String KEY_SCREENSHOTS = "pwa.images.screenshots";
    private static final String SERVLET_PATH = "/servlet/plugins/pwa/image?key=";
    private static final String MIME_PNG = "image/png";
    private static final String MIME_JPEG = "image/jpeg";
    private static final String ICON_PREFIX = "icon-";
    private static final String FORM_FACTOR_WIDE = "wide";
    private static final String FORM_FACTOR_NARROW = "narrow";
    private static final Color DEFAULT_BACKGROUND = Color.WHITE;
    private static final ObjectMapper MAPPER = new ObjectMapper( );

    @Inject
    private FileService _fileService;

    /**
     * Gives the uploaded icons, by name (icon-48, icon-192, icon-512, icon-maskable-512)
     *
     * @return the icons, empty when the bundled ones are used
     */
    public Map<String, PwaImage> getIcons( )
    {
        return read( KEY_ICONS, new TypeReference<LinkedHashMap<String, PwaImage>>( )
        {
        } ).orElseGet( LinkedHashMap::new );
    }

    /**
     * Gives the screenshots, in their display order
     *
     * @return the screenshots
     */
    public List<PwaImage> getScreenshots( )
    {
        return read( KEY_SCREENSHOTS, new TypeReference<ArrayList<PwaImage>>( )
        {
        } ).orElseGet( ArrayList::new );
    }

    /**
     * Generates and stores the icons from an uploaded image, replacing the previous ones
     *
     * @param content
     *            the uploaded image
     * @param strBackgroundColor
     *            the background color of the maskable icon
     * @throws PwaImageException
     *             if the image is refused or cannot be stored
     */
    public void setIcon( byte [ ] content, String strBackgroundColor ) throws PwaImageException
    {
        BufferedImage source = readImage( content );

        if ( Math.min( source.getWidth( ), source.getHeight( ) ) < MIN_ICON_SIZE )
        {
            throw new PwaImageException( MESSAGE_ICON_TOO_SMALL );
        }

        Map<String, PwaImage> icons = new LinkedHashMap<>( );

        try
        {
            for ( int nSize : ManifestBuilder.ICON_SIZES )
            {
                icons.put( ICON_PREFIX + nSize, store( IconGenerator.icon( source, nSize ), MIME_PNG, nSize, nSize, null ) );
            }
            icons.put( ICON_MASKABLE, store( IconGenerator.maskable( source, MIN_ICON_SIZE, IconGenerator.parseColor( strBackgroundColor, DEFAULT_BACKGROUND ) ),
                    MIME_PNG, MIN_ICON_SIZE, MIN_ICON_SIZE, null ) );
        }
        catch( IOException | FileServiceException e )
        {
            icons.values( ).forEach( this::delete );
            throw new PwaImageException( MESSAGE_STORE_FAILED, e );
        }

        Map<String, PwaImage> previous = getIcons( );
        write( KEY_ICONS, icons );
        previous.values( ).forEach( this::delete );
    }

    /**
     * Removes the uploaded icons: the bundled ones are used again
     */
    public void resetIcon( )
    {
        Map<String, PwaImage> previous = getIcons( );
        DatastoreService.removeData( KEY_ICONS );
        previous.values( ).forEach( this::delete );
    }

    /**
     * Stores a screenshot
     *
     * @param content
     *            the uploaded image
     * @param strLabel
     *            the accessible label of the screenshot
     * @throws PwaImageException
     *             if the image is refused or cannot be stored
     */
    public void addScreenshot( byte [ ] content, String strLabel ) throws PwaImageException
    {
        List<PwaImage> listScreenshots = getScreenshots( );

        if ( listScreenshots.size( ) >= MAX_SCREENSHOTS )
        {
            throw new PwaImageException( MESSAGE_SCREENSHOT_LIMIT );
        }

        BufferedImage image = readImage( content );
        int nMin = Math.min( image.getWidth( ), image.getHeight( ) );
        int nMax = Math.max( image.getWidth( ), image.getHeight( ) );

        if ( nMin < MIN_SCREENSHOT_SIZE || nMax > MAX_SCREENSHOT_SIZE )
        {
            throw new PwaImageException( MESSAGE_SCREENSHOT_SIZE );
        }
        if ( (double) nMax / nMin > MAX_SCREENSHOT_RATIO )
        {
            throw new PwaImageException( MESSAGE_SCREENSHOT_RATIO );
        }

        try
        {
            String strMimeType = IconGenerator.FORMAT_PNG.equals( IconGenerator.getFormat( content ) ) ? MIME_PNG : MIME_JPEG;
            listScreenshots.add( store( content, strMimeType, image.getWidth( ), image.getHeight( ), StringUtils.trimToNull( strLabel ) ) );
        }
        catch( IOException | FileServiceException e )
        {
            throw new PwaImageException( MESSAGE_STORE_FAILED, e );
        }

        write( KEY_SCREENSHOTS, listScreenshots );
    }

    /**
     * Removes a screenshot
     *
     * @param strFileKey
     *            the file key of the screenshot
     * @return true if the screenshot existed
     */
    public boolean removeScreenshot( String strFileKey )
    {
        List<PwaImage> listScreenshots = getScreenshots( );
        Optional<PwaImage> screenshot = listScreenshots.stream( ).filter( image -> image.getFileKey( ).equals( strFileKey ) ).findFirst( );

        screenshot.ifPresent( image -> {
            listScreenshots.remove( image );
            write( KEY_SCREENSHOTS, listScreenshots );
            delete( image );
        } );

        return screenshot.isPresent( );
    }

    /**
     * Finds an image of the progressive web app by its file key: only the files registered as icons or screenshots are given
     *
     * @param strFileKey
     *            the file key
     * @return the image
     */
    public Optional<PwaImage> findImage( String strFileKey )
    {
        if ( StringUtils.isBlank( strFileKey ) )
        {
            return Optional.empty( );
        }

        List<PwaImage> listImages = new ArrayList<>( getIcons( ).values( ) );
        listImages.addAll( getScreenshots( ) );

        return listImages.stream( ).filter( image -> strFileKey.equals( image.getFileKey( ) ) ).findFirst( );
    }

    /**
     * Gives the content of an image
     *
     * @param image
     *            the image
     * @return the bytes
     * @throws FileServiceException
     *             if the file cannot be read
     * @throws IOException
     *             if the file cannot be read
     */
    public byte [ ] getContent( PwaImage image ) throws FileServiceException, IOException
    {
        try ( var input = getProvider( ).getInputStream( image.getFileKey( ) ) )
        {
            return input.readAllBytes( );
        }
    }

    /**
     * Gives the URL of an image
     *
     * @param strContextPath
     *            the context path of the webapp
     * @param image
     *            the image
     * @return the URL, absolute path
     */
    public static String getUrl( String strContextPath, PwaImage image )
    {
        return StringUtils.defaultString( strContextPath ) + SERVLET_PATH + image.getFileKey( );
    }

    /**
     * Gives the icons of the manifest when icons were uploaded
     *
     * @param strContextPath
     *            the context path of the webapp
     * @return the icons, null to use the bundled ones
     */
    public List<Map<String, String>> getManifestIcons( String strContextPath )
    {
        Map<String, PwaImage> icons = getIcons( );

        if ( icons.isEmpty( ) )
        {
            return null;
        }

        List<Map<String, String>> listIcons = new ArrayList<>( );
        icons.forEach( ( strName, image ) -> {
            Map<String, String> icon = new LinkedHashMap<>( );
            icon.put( "src", getUrl( strContextPath, image ) );
            icon.put( "sizes", image.getWidth( ) + "x" + image.getHeight( ) );
            icon.put( "type", image.getMimeType( ) );
            icon.put( "purpose", ICON_MASKABLE.equals( strName ) ? "maskable" : "any" );
            listIcons.add( icon );
        } );

        return listIcons;
    }

    /**
     * Gives the screenshots of the manifest
     *
     * @param strContextPath
     *            the context path of the webapp
     * @return the screenshots
     */
    public List<Map<String, String>> getManifestScreenshots( String strContextPath )
    {
        List<Map<String, String>> listScreenshots = new ArrayList<>( );

        for ( PwaImage image : getScreenshots( ) )
        {
            Map<String, String> screenshot = new LinkedHashMap<>( );
            screenshot.put( "src", getUrl( strContextPath, image ) );
            screenshot.put( "sizes", image.getWidth( ) + "x" + image.getHeight( ) );
            screenshot.put( "type", image.getMimeType( ) );
            screenshot.put( "form_factor", image.getWidth( ) > image.getHeight( ) ? FORM_FACTOR_WIDE : FORM_FACTOR_NARROW );
            if ( image.getLabel( ) != null )
            {
                screenshot.put( "label", image.getLabel( ) );
            }
            listScreenshots.add( screenshot );
        }

        return listScreenshots;
    }

    /**
     * Reads an uploaded image
     *
     * @param content
     *            the bytes
     * @return the image
     * @throws PwaImageException
     *             if the bytes are not a PNG or JPEG image, are too large, or declare dimensions too large to be decoded safely
     */
    private static BufferedImage readImage( byte [ ] content ) throws PwaImageException
    {
        if ( content != null && content.length > MAX_UPLOAD_BYTES )
        {
            throw new PwaImageException( MESSAGE_TOO_LARGE );
        }

        try
        {
            int [ ] size = IconGenerator.getSize( content );
            if ( size != null && ( size [0] > MAX_IMAGE_SIZE || size [1] > MAX_IMAGE_SIZE ) )
            {
                throw new PwaImageException( MESSAGE_DIMENSIONS_TOO_LARGE );
            }

            BufferedImage image = IconGenerator.read( content );
            if ( image == null )
            {
                throw new PwaImageException( MESSAGE_NOT_AN_IMAGE );
            }

            return image;
        }
        catch( IOException e )
        {
            throw new PwaImageException( MESSAGE_NOT_AN_IMAGE, e );
        }
    }

    /**
     * Stores a file
     *
     * @param content
     *            the bytes
     * @param strMimeType
     *            the MIME type
     * @param nWidth
     *            the width
     * @param nHeight
     *            the height
     * @param strLabel
     *            the label, or null
     * @return the stored image
     * @throws FileServiceException
     *             if the file cannot be stored
     */
    private PwaImage store( byte [ ] content, String strMimeType, int nWidth, int nHeight, String strLabel ) throws FileServiceException
    {
        PwaImage image = new PwaImage( );
        image.setFileKey( getProvider( ).storeBytes( content ) );
        image.setMimeType( strMimeType );
        image.setWidth( nWidth );
        image.setHeight( nHeight );
        image.setLabel( strLabel );

        return image;
    }

    /**
     * Deletes the file of an image, logging a failure: the image is no longer referenced anyway
     *
     * @param image
     *            the image
     */
    private void delete( PwaImage image )
    {
        try
        {
            getProvider( ).delete( image.getFileKey( ) );
        }
        catch( FileServiceException e )
        {
            AppLogService.error( "Unable to delete the PWA image {}", image.getFileKey( ), e );
        }
    }

    /**
     * @return the file store of the core
     */
    private IFileStoreServiceProvider getProvider( )
    {
        return _fileService.getFileStoreServiceProvider( );
    }

    /**
     * Reads a JSON value of the datastore
     *
     * @param <T>
     *            the type of the value
     * @param strKey
     *            the datastore key
     * @param type
     *            the type of the value
     * @return the value, empty when it is missing or unreadable
     */
    private static <T> Optional<T> read( String strKey, TypeReference<T> type )
    {
        String strJson = DatastoreService.getDataValue( strKey, null );

        if ( StringUtils.isBlank( strJson ) )
        {
            return Optional.empty( );
        }

        try
        {
            return Optional.ofNullable( MAPPER.readValue( strJson, type ) );
        }
        catch( JsonProcessingException e )
        {
            AppLogService.error( "Unreadable PWA images in the datastore key {}", strKey, e );
            return Optional.empty( );
        }
    }

    /**
     * Writes a value as JSON in the datastore
     *
     * @param strKey
     *            the datastore key
     * @param value
     *            the value
     */
    private static void write( String strKey, Object value )
    {
        try
        {
            DatastoreService.setDataValue( strKey, MAPPER.writeValueAsString( value ) );
        }
        catch( JsonProcessingException e )
        {
            throw new IllegalStateException( "Unable to write the PWA images", e );
        }
    }
}
