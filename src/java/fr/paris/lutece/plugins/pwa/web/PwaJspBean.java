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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import fr.paris.lutece.plugins.pwa.service.ManifestBuilder;
import fr.paris.lutece.plugins.pwa.service.PwaImage;
import fr.paris.lutece.plugins.pwa.service.PwaImageException;
import fr.paris.lutece.plugins.pwa.service.PwaImageService;
import fr.paris.lutece.plugins.pwa.service.PwaSettingsService;
import fr.paris.lutece.portal.service.datastore.DatastoreService;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.message.AdminMessage;
import fr.paris.lutece.portal.service.message.AdminMessageService;
import fr.paris.lutece.portal.service.upload.MultipartItem;
import fr.paris.lutece.portal.util.mvc.admin.MVCAdminJspBean;
import fr.paris.lutece.portal.util.mvc.admin.annotations.Controller;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.portal.util.mvc.utils.MVCMessage;
import fr.paris.lutece.portal.util.mvc.utils.MVCUtils;
import fr.paris.lutece.portal.web.upload.MultipartHttpServletRequest;
import fr.paris.lutece.portal.web.cdi.mvc.Models;
import fr.paris.lutece.util.ReferenceList;
import fr.paris.lutece.util.url.UrlItem;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Back office of the progressive web app: the settings of the manifest, the icon and the screenshots, with a preview of the manifest and the installability
 * checklist
 */
@RequestScoped
@Named( "pwaJspBean" )
@Controller( controllerJsp = "ManagePwa.jsp", controllerPath = "jsp/admin/plugins/pwa/", right = PwaJspBean.RIGHT_MANAGE_PWA, securityTokenEnabled = true )
public class PwaJspBean extends MVCAdminJspBean
{
    public static final String RIGHT_MANAGE_PWA = "PWA_MANAGEMENT";

    private static final long serialVersionUID = 1L;

    private static final String TEMPLATE_MANAGE_SETTINGS = "/admin/plugins/pwa/manage_settings.html";
    private static final String TEMPLATE_MODIFY_ICON = "/admin/plugins/pwa/modify_icon.html";
    private static final String TEMPLATE_MANAGE_SCREENSHOTS = "/admin/plugins/pwa/manage_screenshots.html";

    private static final String VIEW_MANAGE_SETTINGS = "manageSettings";
    private static final String VIEW_MODIFY_ICON = "modifyIcon";
    private static final String VIEW_CONFIRM_RESET_ICON = "confirmResetIcon";
    private static final String VIEW_MANAGE_SCREENSHOTS = "manageScreenshots";
    private static final String VIEW_CONFIRM_REMOVE_SCREENSHOT = "confirmRemoveScreenshot";

    private static final String ACTION_MODIFY_SETTINGS = "modifySettings";
    private static final String ACTION_MODIFY_ICON = "modifyIcon";
    private static final String ACTION_RESET_ICON = "resetIcon";
    private static final String ACTION_ADD_SCREENSHOT = "addScreenshot";
    private static final String ACTION_REMOVE_SCREENSHOT = "removeScreenshot";

    private static final String PROPERTY_PAGE_TITLE_SETTINGS = "pwa.manage_settings.pageTitle";
    private static final String PROPERTY_PAGE_TITLE_ICON = "pwa.modify_icon.pageTitle";
    private static final String PROPERTY_PAGE_TITLE_SCREENSHOTS = "pwa.manage_screenshots.pageTitle";

    private static final String MESSAGE_ORIENTATION_NONE = "pwa.manage_settings.orientation.none";
    private static final String MESSAGE_SETTINGS_SAVED = "pwa.message.settings.saved";
    private static final String MESSAGE_INVALID_COLOR = "pwa.message.settings.invalidColor";
    private static final String MESSAGE_INVALID_DISPLAY = "pwa.message.settings.invalidDisplay";
    private static final String MESSAGE_INVALID_ORIENTATION = "pwa.message.settings.invalidOrientation";
    private static final String MESSAGE_ICON_SAVED = "pwa.message.icon.saved";
    private static final String MESSAGE_NO_FILE = "pwa.message.image.noFile";
    private static final String MESSAGE_CONFIRM_RESET_ICON = "pwa.message.icon.confirmReset";
    private static final String MESSAGE_ICON_RESET = "pwa.message.icon.reset";
    private static final String MESSAGE_SCREENSHOT_ADDED = "pwa.message.screenshot.added";
    private static final String MESSAGE_CONFIRM_REMOVE_SCREENSHOT = "pwa.message.screenshot.confirmRemove";
    private static final String MESSAGE_SCREENSHOT_REMOVED = "pwa.message.screenshot.removed";
    private static final String MESSAGE_SCREENSHOT_NOT_FOUND = "pwa.message.screenshot.notFound";

    private static final String PARAMETER_ICON_FILE = "icon_file";
    private static final String PARAMETER_SCREENSHOT_FILE = "screenshot_file";
    private static final String PARAMETER_SCREENSHOT_LABEL = "screenshot_label";
    private static final String PARAMETER_FILE_KEY = "file_key";

    private static final String MARK_SETTINGS = "settings";
    private static final String MARK_DISPLAY_VALUES = "display_values";
    private static final String MARK_ORIENTATION_VALUES = "orientation_values";
    private static final String MARK_CHECKLIST = "checklist";
    private static final String MARK_MANIFEST = "manifest";
    private static final String MARK_ICONS = "icons";
    private static final String MARK_CUSTOM_ICON = "custom_icon";
    private static final String MARK_SCREENSHOTS = "screenshots";
    private static final String MARK_MAX_SCREENSHOTS = "max_screenshots";
    private static final String MARK_BACKGROUND_COLOR = "background_color";

    private static final String ICON_PREVIEW_MASKABLE = "maskable";
    private static final String BUNDLED_ICONS_PATH = "themes/shared/plugins/pwa/images/";
    private static final String DEFAULT_BACKGROUND_COLOR = "#ffffff";

    @Inject
    private PwaSettingsService _settingsService;

    @Inject
    private PwaImageService _imageService;

    /**
     * Gives the settings form, with the installability checklist and a preview of the manifest
     *
     * @param request
     *            the request
     * @param model
     *            the model
     * @return the page
     */
    @View( value = VIEW_MANAGE_SETTINGS, defaultView = true, securityTokenAction = ACTION_MODIFY_SETTINGS )
    public String getManageSettings( HttpServletRequest request, Models model )
    {
        if ( model.get( MARK_SETTINGS ) == null )
        {
            model.put( MARK_SETTINGS, _settingsService.getSettings( ) );
        }
        ReferenceList listDisplays = new ReferenceList( );
        ManifestBuilder.DISPLAY_VALUES.forEach( strValue -> listDisplays.addItem( strValue, strValue ) );
        ReferenceList listOrientations = new ReferenceList( );
        listOrientations.addItem( StringUtils.EMPTY, I18nService.getLocalizedString( MESSAGE_ORIENTATION_NONE, getLocale( ) ) );
        ManifestBuilder.ORIENTATION_VALUES.forEach( strValue -> listOrientations.addItem( strValue, strValue ) );
        model.put( MARK_DISPLAY_VALUES, listDisplays );
        model.put( MARK_ORIENTATION_VALUES, listOrientations );
        model.put( MARK_CHECKLIST, _settingsService.getChecklist( request ) );

        String strContextPath = request.getContextPath( );
        model.put( MARK_MANIFEST, ManifestBuilder.build( strContextPath, I18nService.getDefaultLocale( ).toLanguageTag( ),
                strKey -> DatastoreService.getDataValue( strKey, null ), _imageService.getManifestIcons( strContextPath ),
                _imageService.getManifestScreenshots( strContextPath ) ) );

        return getPage( PROPERTY_PAGE_TITLE_SETTINGS, TEMPLATE_MANAGE_SETTINGS, model );
    }

    /**
     * Saves the settings, or gives the form back with the errors and the values entered
     *
     * @param request
     *            the request
     * @param model
     *            the model
     * @return the next page
     */
    @Action( ACTION_MODIFY_SETTINGS )
    public String doModifySettings( HttpServletRequest request, Models model )
    {
        Map<String, String> settings = new LinkedHashMap<>( );
        PwaSettingsService.KEYS.forEach( strKey -> settings.put( strKey,
                StringUtils.trimToEmpty( StringEscapeUtils.unescapeHtml4( request.getParameter( strKey ) ) ) ) );

        List<String> listErrors = validate( settings );

        if ( !listErrors.isEmpty( ) )
        {
            List<MVCMessage> listMessages = new ArrayList<>( );
            listErrors.forEach( strKey -> listMessages.add( new MVCMessage( I18nService.getLocalizedString( strKey, getLocale( ) ) ) ) );
            model.put( MVCUtils.MARK_ERRORS, listMessages );
            model.put( MARK_SETTINGS, settings );

            return getManageSettings( request, model );
        }

        _settingsService.saveSettings( settings );
        addInfo( MESSAGE_SETTINGS_SAVED, getLocale( ) );

        return redirectView( request, VIEW_MANAGE_SETTINGS );
    }

    /**
     * Gives the icon form, with the current icons
     *
     * @param request
     *            the request
     * @param model
     *            the model
     * @return the page
     */
    @View( value = VIEW_MODIFY_ICON )
    public String getModifyIcon( HttpServletRequest request, Models model )
    {
        Map<String, PwaImage> icons = _imageService.getIcons( );
        Map<String, String> previews = new LinkedHashMap<>( );

        if ( icons.isEmpty( ) )
        {
            for ( int nSize : ManifestBuilder.ICON_SIZES )
            {
                previews.put( String.valueOf( nSize ), BUNDLED_ICONS_PATH + "icon-" + nSize + ".png" );
            }
            previews.put( ICON_PREVIEW_MASKABLE, BUNDLED_ICONS_PATH + "icon-maskable-512.png" );
        }
        else
        {
            icons.forEach( ( strName, image ) -> previews.put(
                    PwaImageService.ICON_MASKABLE.equals( strName ) ? ICON_PREVIEW_MASKABLE : String.valueOf( image.getWidth( ) ),
                    PwaImageService.getUrl( request.getContextPath( ), image ) ) );
        }

        model.put( MARK_ICONS, previews );
        model.put( MARK_CUSTOM_ICON, !icons.isEmpty( ) );
        model.put( MARK_BACKGROUND_COLOR, ManifestBuilder.toCssColor( DatastoreService.getDataValue( ManifestBuilder.KEY_BACKGROUND_COLOR, null ),
                DEFAULT_BACKGROUND_COLOR ) );

        return getPage( PROPERTY_PAGE_TITLE_ICON, TEMPLATE_MODIFY_ICON, model );
    }

    /**
     * Generates the icons from the uploaded image
     *
     * @param request
     *            the request
     * @return the next page
     */
    @Action( ACTION_MODIFY_ICON )
    public String doModifyIcon( HttpServletRequest request )
    {
        byte [ ] content = getUploadedFile( request, PARAMETER_ICON_FILE );

        if ( content == null )
        {
            addError( MESSAGE_NO_FILE, getLocale( ) );
            return redirectView( request, VIEW_MODIFY_ICON );
        }

        try
        {
            _imageService.setIcon( content, DatastoreService.getDataValue( ManifestBuilder.KEY_BACKGROUND_COLOR, null ) );
            addInfo( MESSAGE_ICON_SAVED, getLocale( ) );
        }
        catch( PwaImageException e )
        {
            addError( e.getMessage( ), getLocale( ) );
        }

        return redirectView( request, VIEW_MODIFY_ICON );
    }

    /**
     * Asks to confirm going back to the bundled icons
     *
     * @param request
     *            the request
     * @return the confirmation page
     */
    @View( value = VIEW_CONFIRM_RESET_ICON, securityTokenAction = ACTION_RESET_ICON )
    public String getConfirmResetIcon( HttpServletRequest request )
    {
        return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_CONFIRM_RESET_ICON, getActionUrl( ACTION_RESET_ICON ),
                AdminMessage.TYPE_CONFIRMATION ) );
    }

    /**
     * Removes the uploaded icons
     *
     * @param request
     *            the request
     * @return the next page
     */
    @Action( ACTION_RESET_ICON )
    public String doResetIcon( HttpServletRequest request )
    {
        _imageService.resetIcon( );
        addInfo( MESSAGE_ICON_RESET, getLocale( ) );

        return redirectView( request, VIEW_MODIFY_ICON );
    }

    /**
     * Gives the screenshots and the form to add one
     *
     * @param request
     *            the request
     * @param model
     *            the model
     * @return the page
     */
    @View( value = VIEW_MANAGE_SCREENSHOTS, securityTokenAction = ACTION_ADD_SCREENSHOT )
    public String getManageScreenshots( HttpServletRequest request, Models model )
    {
        List<Map<String, Object>> listScreenshots = new ArrayList<>( );

        for ( PwaImage image : _imageService.getScreenshots( ) )
        {
            Map<String, Object> screenshot = new LinkedHashMap<>( );
            screenshot.put( "url", PwaImageService.getUrl( request.getContextPath( ), image ) );
            screenshot.put( "fileKey", image.getFileKey( ) );
            screenshot.put( "width", image.getWidth( ) );
            screenshot.put( "height", image.getHeight( ) );
            screenshot.put( "wide", image.getWidth( ) > image.getHeight( ) );
            screenshot.put( "label", StringUtils.defaultString( image.getLabel( ) ) );
            listScreenshots.add( screenshot );
        }

        model.put( MARK_SCREENSHOTS, listScreenshots );
        model.put( MARK_MAX_SCREENSHOTS, PwaImageService.MAX_SCREENSHOTS );

        return getPage( PROPERTY_PAGE_TITLE_SCREENSHOTS, TEMPLATE_MANAGE_SCREENSHOTS, model );
    }

    /**
     * Adds a screenshot
     *
     * @param request
     *            the request
     * @return the next page
     */
    @Action( ACTION_ADD_SCREENSHOT )
    public String doAddScreenshot( HttpServletRequest request )
    {
        byte [ ] content = getUploadedFile( request, PARAMETER_SCREENSHOT_FILE );

        if ( content == null )
        {
            addError( MESSAGE_NO_FILE, getLocale( ) );
            return redirectView( request, VIEW_MANAGE_SCREENSHOTS );
        }

        try
        {
            _imageService.addScreenshot( content, StringEscapeUtils.unescapeHtml4( request.getParameter( PARAMETER_SCREENSHOT_LABEL ) ) );
            addInfo( MESSAGE_SCREENSHOT_ADDED, getLocale( ) );
        }
        catch( PwaImageException e )
        {
            addError( e.getMessage( ), getLocale( ) );
        }

        return redirectView( request, VIEW_MANAGE_SCREENSHOTS );
    }

    /**
     * Asks to confirm the removal of a screenshot
     *
     * @param request
     *            the request
     * @return the confirmation page
     */
    @View( value = VIEW_CONFIRM_REMOVE_SCREENSHOT, securityTokenAction = ACTION_REMOVE_SCREENSHOT )
    public String getConfirmRemoveScreenshot( HttpServletRequest request )
    {
        String strFileKey = request.getParameter( PARAMETER_FILE_KEY );

        if ( _imageService.getScreenshots( ).stream( ).noneMatch( image -> image.getFileKey( ).equals( strFileKey ) ) )
        {
            return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_SCREENSHOT_NOT_FOUND, AdminMessage.TYPE_STOP ) );
        }

        UrlItem url = new UrlItem( getActionUrl( ACTION_REMOVE_SCREENSHOT ) );
        url.addParameter( PARAMETER_FILE_KEY, strFileKey );

        return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_CONFIRM_REMOVE_SCREENSHOT, url.getUrl( ), AdminMessage.TYPE_CONFIRMATION ) );
    }

    /**
     * Removes a screenshot
     *
     * @param request
     *            the request
     * @return the next page
     */
    @Action( ACTION_REMOVE_SCREENSHOT )
    public String doRemoveScreenshot( HttpServletRequest request )
    {
        if ( _imageService.removeScreenshot( request.getParameter( PARAMETER_FILE_KEY ) ) )
        {
            addInfo( MESSAGE_SCREENSHOT_REMOVED, getLocale( ) );
        }
        else
        {
            addError( MESSAGE_SCREENSHOT_NOT_FOUND, getLocale( ) );
        }

        return redirectView( request, VIEW_MANAGE_SCREENSHOTS );
    }

    /**
     * Validates the settings
     *
     * @param settings
     *            the settings entered
     * @return the i18n keys of the errors, empty when the settings are valid
     */
    private static List<String> validate( Map<String, String> settings )
    {
        List<String> listErrors = new ArrayList<>( );

        for ( String strKey : List.of( ManifestBuilder.KEY_BACKGROUND_COLOR, ManifestBuilder.KEY_THEME_COLOR ) )
        {
            String strColor = settings.get( strKey );
            if ( !strColor.isEmpty( ) && ManifestBuilder.toCssColor( strColor, null ) == null )
            {
                listErrors.add( MESSAGE_INVALID_COLOR );
                break;
            }
        }
        if ( !ManifestBuilder.DISPLAY_VALUES.contains( settings.get( ManifestBuilder.KEY_DISPLAY ) ) )
        {
            listErrors.add( MESSAGE_INVALID_DISPLAY );
        }
        String strOrientation = settings.get( ManifestBuilder.KEY_ORIENTATION );
        if ( !strOrientation.isEmpty( ) && !ManifestBuilder.ORIENTATION_VALUES.contains( strOrientation ) )
        {
            listErrors.add( MESSAGE_INVALID_ORIENTATION );
        }

        return listErrors;
    }

    /**
     * Gives the content of an uploaded file
     *
     * @param request
     *            the request
     * @param strParameter
     *            the name of the file field
     * @return the bytes, or null when no file was sent
     */
    private static byte [ ] getUploadedFile( HttpServletRequest request, String strParameter )
    {
        if ( request instanceof MultipartHttpServletRequest multipartRequest )
        {
            MultipartItem item = multipartRequest.getFile( strParameter );
            if ( item != null && item.getSize( ) > 0 )
            {
                return item.get( );
            }
        }

        return null;
    }
}
