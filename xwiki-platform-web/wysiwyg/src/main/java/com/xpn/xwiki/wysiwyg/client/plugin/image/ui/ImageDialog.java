package com.xpn.xwiki.wysiwyg.client.plugin.image.ui;

import java.util.Map;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormHandler;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormSubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormSubmitEvent;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.image.ImageHTMLGenerator;
import com.xpn.xwiki.wysiwyg.client.widget.CompositeDialogBox;

/**
 * Dialog for the image insert plugin, to handle choosing an image to insert from the existing images or uploading a new
 * one.
 * 
 * @version $Id$
 */
public class ImageDialog extends CompositeDialogBox implements ClickListener, FormHandler
{
    /**
     * The HTML block for the image to be inserted. Will be generated by the {@link ImageHTMLGenerator} and returned to
     * the image plugin on dialog close.
     */
    private String imageHTMLBlock;

    /**
     * The file upload input used for the new images upload.
     */
    private FileUpload fileUploadInput;

    /**
     * Panel with the existing images previews.
     */
    private FlowPanel imagesPanel;

    /**
     * The wiki of the document we are currently editing.
     */
    private String currentWiki;

    /**
     * The space of the document we are currently editing.
     */
    private String currentSpace;

    /**
     * The page name of the document we are currently editing.
     */
    private String currentPage;

    /**
     * Creates an image insertion dialog, for the referred page, and the default file upload url.
     * 
     * @param currentWiki the wiki of the document we are currently editing.
     * @param currentSpace the space of the document we are currently editing.
     * @param currentPage the page of the document we are currently editing.
     * @param fileUploadURL the url where the new attached images should be uploaded.
     */
    public ImageDialog(String currentWiki, String currentSpace, String currentPage, String fileUploadURL)
    {
        super(false, true);
        getDialog().setText(Strings.INSTANCE.image());
        FlowPanel mainPanel = new FlowPanel();

        this.currentWiki = currentWiki;
        this.currentSpace = currentSpace;
        this.currentPage = currentPage;

        mainPanel.add(getImagesPanel());
        mainPanel.add(getFileUploadForm(fileUploadURL, "filepath"));
        mainPanel.addStyleName("xImageDialogMain");

        initWidget(mainPanel);
    }

    /**
     * @param uploadURL the URL to which the files are uploaded
     * @param fileInputName the name of the file input in the file upload form
     * @return the upload form for the attached file
     */
    private FormPanel getFileUploadForm(String uploadURL, String fileInputName)
    {
        final FormPanel uploadForm = new FormPanel();
        uploadForm.addStyleName("xUploadForm");
        uploadForm.setAction(uploadURL);
        uploadForm.setEncoding(FormPanel.ENCODING_MULTIPART);
        uploadForm.setMethod(FormPanel.METHOD_POST);
        FlowPanel panel = new FlowPanel();
        uploadForm.setWidget(panel);

        panel.add(new Label(Strings.INSTANCE.fileUploadLabel()));

        fileUploadInput = new FileUpload();
        fileUploadInput.setName(fileInputName);
        panel.add(fileUploadInput);

        panel.add(new Button(Strings.INSTANCE.fileUploadSubmitLabel(), new ClickListener()
        {
            public void onClick(Widget sender)
            {
                uploadForm.submit();
            }
        }));

        uploadForm.addFormHandler(this);
        return uploadForm;
    }

    /**
     * {@inheritDoc}
     * 
     * @see FormHandler#onSubmit(FormSubmitEvent)
     */
    public void onSubmit(FormSubmitEvent event)
    {
        if (fileUploadInput.getFilename().trim().length() == 0) {
            Window.alert(Strings.INSTANCE.fileUploadNoPathError());
            event.setCancelled(true);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see FormHandler#onSubmitComplete(FormSubmitCompleteEvent)
     */
    public void onSubmitComplete(FormSubmitCompleteEvent event)
    {
        updateImagesPanel();
    }

    /**
     * @return the panel holding the images preview, for the user to choose an image to insert in the document.
     */
    private Panel getImagesPanel()
    {
        ScrollPanel containerPanel = new ScrollPanel();
        containerPanel.addStyleName("xImagesContainerPanel");
        Panel imageChooserPanel = new FlowPanel();
        imageChooserPanel.addStyleName("xImageChooser");
        Label chooseLabel = new Label(Strings.INSTANCE.fileChooseLabel());
        imagesPanel = new FlowPanel();
        imageChooserPanel.add(chooseLabel);
        containerPanel.add(imagesPanel);
        imageChooserPanel.add(containerPanel);

        updateImagesPanel();

        return imageChooserPanel;

    }

    /**
     * Populates the images panel with the images in the passed map.
     * 
     * @param images the list of images given by their URL and corresponding file names.
     */
    public void populateImagesPanel(Map<String, String> images)
    {
        for (Map.Entry<String, String> imageData : images.entrySet()) {
            final ImagePreviewWidget imageWidget = new ImagePreviewWidget(imageData.getKey(), imageData.getValue());
            imageWidget.addClickListener(ImageDialog.this);
            imagesPanel.add(imageWidget);
        }
        // Add a div for float clear
        Panel clearPanel = new FlowPanel();
        clearPanel.addStyleName("clear");
        imagesPanel.add(clearPanel);
    }

    /**
     * Fetches the new images list and repopulates the images panel with the new images.
     */
    public void updateImagesPanel()
    {
        imagesPanel.clear();
        WysiwygService.Singleton.getInstance().getImageAttachments(currentWiki, currentSpace, currentPage,
            new AsyncCallback<Map<String, String>>()
            {
                public void onFailure(Throwable caught)
                {
                    imagesPanel.add(new HTML(Strings.INSTANCE.fileListFetchError()));
                }

                public void onSuccess(Map<String, String> result)
                {
                    populateImagesPanel(result);
                }
            });
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        if (sender instanceof HasImage) {
            imageHTMLBlock = ImageHTMLGenerator.getInstance().getAttachedImageHTML((HasImage) sender);
            this.hide();
        }
    }

    /**
     * @return the image HTML block built as a result of the user input in this dialog.
     */
    public String getImageHTMLBlock()
    {
        return imageHTMLBlock;
    }
}
