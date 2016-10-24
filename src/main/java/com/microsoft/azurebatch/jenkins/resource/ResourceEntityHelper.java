/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azurebatch.jenkins.resource;

import com.microsoft.azurebatch.jenkins.azurestorage.AzureStorageHelper;
import com.microsoft.azurebatch.jenkins.logger.Logger;
import com.microsoft.azurebatch.jenkins.utils.ZipHelper;
import com.microsoft.windowsazure.storage.StorageException;
import com.microsoft.windowsazure.storage.blob.CloudBlobContainer;
import com.microsoft.windowsazure.storage.blob.CloudBlockBlob;
import hudson.FilePath;
import hudson.model.BuildListener;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

/**
 * ResourceEntityHelper class
 */
public class ResourceEntityHelper {
    
    /**
     * Zip and upload LocalResourceEntity to Storage
     * @param listener BuildListener
     * @param cloudBlobContainer blob container
     * @param sasKey SAS key of container
     * @param resource the local resource
     * @param tempZipFolder the temp folder for zipped files
     * @param deleteZipAfterUpload whether to delete zip file after upload
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws URISyntaxException
     * @throws StorageException
     * @throws InterruptedException
     * @throws InvalidKeyException
     */
    public static void zipAndUploadLocalResourceEntity(BuildListener listener, 
            CloudBlobContainer cloudBlobContainer,
            String sasKey,
            LocalResourceEntity resource, 
            String tempZipFolder,
            boolean deleteZipAfterUpload) throws IllegalArgumentException, IOException, URISyntaxException, StorageException, InterruptedException, InvalidKeyException {
        String srcPath = resource.getSourcePath();
        
        if (resource.requireZip()) {
            srcPath = zipLocalResourceEntity(listener, resource, tempZipFolder);
        }
        
        String blobName = resource.getBlobName();
        CloudBlockBlob blob = cloudBlobContainer.getBlockBlobReference(blobName);
        
        File srcFile = new File(srcPath);
        if (srcFile.length() >= 200 * 1024 * 1024) {
            Logger.log(listener, "Uploading large file %s (size %dM) to Azure storage...", srcPath, srcFile.length() / 1024 / 1024);
        }
        URI uploadedBlobUri = AzureStorageHelper.upload(listener, blob, new FilePath(new File(srcPath)));        
        
        resource.setBlobPath(uploadedBlobUri.toString()+ "?" + sasKey);
        
        if (resource.requireZip() && deleteZipAfterUpload) {
            File file = new File(srcPath);
            file.deleteOnExit();
        }            
    }
    
    private static String zipLocalResourceEntity(BuildListener listener, LocalResourceEntity resource, String outputFolder) throws IllegalArgumentException, IOException {
        if (!resource.requireZip()) {
            throw new IllegalArgumentException();
        }
        
        String outputZipPath = outputFolder + File.separator + resource.getResourceName() + ".zip";
        Logger.log(listener, String.format("Zipping %s to %s...", resource.getSourcePath(), outputZipPath));
        ZipHelper.zipFolder(resource.getSourcePath(), outputZipPath);
        
        return outputZipPath;
    }    
}
