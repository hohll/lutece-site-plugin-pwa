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
import java.util.Optional;

import fr.paris.lutece.plugins.pwa.service.PwaImage;
import fr.paris.lutece.plugins.pwa.service.PwaImageService;
import fr.paris.lutece.portal.service.file.FileServiceException;
import fr.paris.lutece.portal.service.util.AppLogService;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Serves the uploaded icons and screenshots of the progressive web app. Only the files registered as such are served, never another file of the file store.
 */
public class ImageServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;
    private static final String PARAMETER_KEY = "key";
    private static final String HEADER_CACHE_CONTROL = "Cache-Control";
    private static final String CACHE_CONTROL = "public, max-age=604800";
    private static final String HEADER_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    private static final String NOSNIFF = "nosniff";

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        PwaImageService imageService = CDI.current( ).select( PwaImageService.class ).get( );
        Optional<PwaImage> image = imageService.findImage( request.getParameter( PARAMETER_KEY ) );

        if ( image.isEmpty( ) )
        {
            response.sendError( HttpServletResponse.SC_NOT_FOUND );
            return;
        }

        byte [ ] content;
        try
        {
            content = imageService.getContent( image.get( ) );
        }
        catch( FileServiceException e )
        {
            AppLogService.error( "Unable to read the PWA image {}", image.get( ).getFileKey( ), e );
            response.sendError( HttpServletResponse.SC_NOT_FOUND );
            return;
        }

        response.setContentType( image.get( ).getMimeType( ) );
        response.setHeader( HEADER_CACHE_CONTROL, CACHE_CONTROL );
        response.setHeader( HEADER_CONTENT_TYPE_OPTIONS, NOSNIFF );
        response.setContentLength( content.length );
        response.getOutputStream( ).write( content );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServletInfo( )
    {
        return "This servlet delivers the uploaded icons and screenshots of the progressive web app";
    }
}
