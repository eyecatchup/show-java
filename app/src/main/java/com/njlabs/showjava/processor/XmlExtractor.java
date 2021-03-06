package com.njlabs.showjava.processor;

import com.njlabs.showjava.utils.SourceInfo;

import net.dongliu.apk.parser.ApkParser;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by Niranjan on 30-05-2015.
 */
public class XmlExtractor extends ProcessServiceHelper {

    ApkParser apkParser;

    public XmlExtractor(ProcessService processService) {
        this.processService = processService;
        this.UIHandler = processService.UIHandler;
        this.packageFilePath = processService.packageFilePath;
        this.packageName = processService.packageName;
        this.exceptionHandler = processService.exceptionHandler;
        this.apkParser = processService.apkParser;
    }

    public void extract() {

        broadcastStatus("xml");
        ThreadGroup group = new ThreadGroup("XML Extraction Group");
        Thread xmlExtractionThread = new Thread(group, new Runnable(){
            @Override
            public void run(){
                try {
                    ZipFile zipFile = new ZipFile(packageFilePath);
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();

                    while (entries.hasMoreElements()) {
                        ZipEntry zipEntry = entries.nextElement();
                        if(!zipEntry.isDirectory()&&!zipEntry.getName().equals("AndroidManifest.xml")&&FilenameUtils.getExtension(zipEntry.getName()).equals("xml")){
                            broadcastStatus("progress_stream",zipEntry.getName());
                            writeXML(zipEntry.getName());
                        }
                    }
                    zipFile.close();
                    allDone();
                } catch(Exception | StackOverflowError e) {
                    processService.publishProgress("start_activity_with_error");
                }
            }
        },"XML Extraction Thread", 20971520);
        xmlExtractionThread.setPriority(Thread.MAX_PRIORITY);
        xmlExtractionThread.setUncaughtExceptionHandler(exceptionHandler);
        xmlExtractionThread.start();

    }

    private void writeXML(String path) {
        try {
            String xml = apkParser.transBinaryXml(path);
            String fileFolderPath = processService.sourceOutputDir + "/" + path.replace(FilenameUtils.getName(path), "");
            File fileFolder = new File(fileFolderPath);
            if (!fileFolder.isDirectory()) {
                fileFolder.mkdirs();
            }
            FileUtils.writeStringToFile(new File(fileFolderPath + FilenameUtils.getName(path)), xml);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void allDone(){
        SourceInfo.setXmlSourceStatus(processService,true);
        processService.broadcastStatus("start_activity");
    }
}
