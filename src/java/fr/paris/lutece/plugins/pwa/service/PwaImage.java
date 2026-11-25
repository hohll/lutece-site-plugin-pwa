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

import java.io.Serializable;

/**
 * An image of the progressive web app stored in the file store of the core: an icon or a screenshot
 */
public class PwaImage implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String _strFileKey;
    private String _strMimeType;
    private int _nWidth;
    private int _nHeight;
    private String _strLabel;

    /**
     * @return the key of the file in the file store
     */
    public String getFileKey( )
    {
        return _strFileKey;
    }

    /**
     * @param strFileKey
     *            the key of the file in the file store
     */
    public void setFileKey( String strFileKey )
    {
        _strFileKey = strFileKey;
    }

    /**
     * @return the MIME type
     */
    public String getMimeType( )
    {
        return _strMimeType;
    }

    /**
     * @param strMimeType
     *            the MIME type
     */
    public void setMimeType( String strMimeType )
    {
        _strMimeType = strMimeType;
    }

    /**
     * @return the width in pixels
     */
    public int getWidth( )
    {
        return _nWidth;
    }

    /**
     * @param nWidth
     *            the width in pixels
     */
    public void setWidth( int nWidth )
    {
        _nWidth = nWidth;
    }

    /**
     * @return the height in pixels
     */
    public int getHeight( )
    {
        return _nHeight;
    }

    /**
     * @param nHeight
     *            the height in pixels
     */
    public void setHeight( int nHeight )
    {
        _nHeight = nHeight;
    }

    /**
     * @return the accessible label of a screenshot
     */
    public String getLabel( )
    {
        return _strLabel;
    }

    /**
     * @param strLabel
     *            the accessible label of a screenshot
     */
    public void setLabel( String strLabel )
    {
        _strLabel = strLabel;
    }
}
