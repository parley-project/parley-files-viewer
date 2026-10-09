//
//  GetLaunchData.cpp
//  GetLaunchData
//
//  Created by Icloud Bringardner on 8/31/25.
//

#include <stdio.h>
#include <iostream>
#include <sstream>
#include <string>
#include <ApplicationServices/ApplicationServices.h>
#include <CoreFoundation/CoreFoundation.h>
#include <GetLaunchData.h>
#include "UTIWrapper.hpp"

#include <unistd.h>
#include <limits.h>

std::string getIconPathFromBundleURL(CFURLRef bundleURL) {
    CFBundleRef bundle = CFBundleCreate(nullptr, bundleURL);
    if (!bundle) {
        std::cerr << "Failed to create bundle from URL." << std::endl;
        return "";
    }

    CFStringRef iconName = (CFStringRef)CFBundleGetValueForInfoDictionaryKey(bundle, CFSTR("CFBundleIconFile"));
    if (!iconName) {
        iconName = (CFStringRef)CFBundleGetValueForInfoDictionaryKey(bundle, CFSTR("CFBundleIconName"));
    }

    if (!iconName) {
        std::cerr << "Icon name not found in Info.plist." << std::endl;
        CFRelease(bundle);
        return "";
    }

    char iconBuffer[256];
    CFStringGetCString(iconName, iconBuffer, sizeof(iconBuffer), kCFStringEncodingUTF8);
    std::string iconFile = iconBuffer;
    if (iconFile.find(".icns") == std::string::npos) {
        iconFile += ".icns";
    }

    CFURLRef resourcesURL = CFBundleCopyResourcesDirectoryURL(bundle);
    char resourcesPath[PATH_MAX];
    CFURLGetFileSystemRepresentation(resourcesURL, true, reinterpret_cast<UInt8*>(resourcesPath), PATH_MAX);
    CFRelease(resourcesURL);
    CFRelease(bundle);

    return std::string(resourcesPath) + "/" + iconFile;
}

JNIEXPORT jstring JNICALL Java_us_bringardner_parley_files_viewer_registry_MacRegistry_getLaunchData(JNIEnv *env, jobject obj, jstring ext){
    CFStringRef        cfhandler = NULL;
    CFArrayRef        cfhandlers = NULL;
    
    int errors=0;
    long count = 0;
    std::stringstream ret;
    const char *name = (env)->GetStringUTFChars(ext,NULL);
    
    LSRolesMask role = kLSRolesAll;
    std::string arg = (env)->GetStringUTFChars(ext,NULL);
    unsigned long pos = arg.find(",");
    //ret << "pos=" << pos << "\n";
    if (pos != std::string::npos) {
        std::string left  = arg.substr(0,pos);
        std::string right = arg.substr(pos+1);
        //ret << "left=" << left << "\n";
        //ret << "right=" << right << "\n";
        name = left.data();
        if(right == "Editor") {
            role = kLSRolesEditor;
        } else if(right == "Viewer") {
            role = kLSRolesViewer;
        } else if(right == "Shell") {
            role = kLSRolesShell;
        } else if(right == "None") {
            role = kLSRolesNone;
        }
    }
       
    
    
    
    
    std::string uti1 = UTIWrapper::getUTIFromExtension(name);

    if (!uti1.empty()) {
         ret << "UTI for " << name << "=" << uti1 << "\n";
    } else {
        ret << "Could not determine UTI for extension." << "\n";
        errors ++;
    }
    
    CFStringRef uti = CFStringCreateWithCString(NULL, uti1.data(), kCFStringEncodingMacRoman);
    cfhandlers = LSCopyAllRoleHandlersForContentType(uti, role );
    if( cfhandlers==NULL) {
        ret << "\tNo handlers found ext=" << name << "\n";
        errors++;
    } else {
        count = CFArrayGetCount( cfhandlers );
        for (int i = 0; i < count; i++ ) {
            cfhandler = (CFStringRef) CFArrayGetValueAtIndex(cfhandlers,i);
            CFErrorRef error = nullptr;

            // Get array of application URLs for the bundle identifier
            CFArrayRef appURLs = LSCopyApplicationURLsForBundleIdentifier(cfhandler, &error);
            if (!appURLs || CFArrayGetCount(appURLs) == 0) {
                errors++;
                ret << "No application found for bundle identifier." << "\n";
                if (error) {
                    CFStringRef errDesc = CFErrorCopyDescription(error);
                    char errBuffer[256];
                    CFStringGetCString(errDesc, errBuffer, sizeof(errBuffer), kCFStringEncodingUTF8);
                    ret << "Error: " << errBuffer << "\n";
                    CFRelease(errDesc);
                    CFRelease(error);
                }
            } else {
                
                // Get the first URL in the array
                CFURLRef appURL = (CFURLRef)CFArrayGetValueAtIndex(appURLs, 0);
                std::string iconPath = getIconPathFromBundleURL(appURL);
                
                char path[PATH_MAX];
                if (CFURLGetFileSystemRepresentation(appURL, true, reinterpret_cast<UInt8*>(path), PATH_MAX)) {
                } else {
                    ret << "Failed to get file system representation." << "\n";
                    errors++;
                }
                // Create bundle reference from URL
                CFBundleRef bundle = CFBundleCreate(nullptr, appURL);
                if (bundle) {
                        // Try to get CFBundleDisplayName first, fallback to CFBundleName
                        CFStringRef appName = (CFStringRef)CFBundleGetValueForInfoDictionaryKey(bundle, CFSTR("CFBundleDisplayName"));
                        if (!appName) {
                            appName = (CFStringRef)CFBundleGetValueForInfoDictionaryKey(bundle, CFSTR("CFBundleName"));
                        }

                        if (appName) {
                            char nameBuffer[256];
                            if (CFStringGetCString(appName, nameBuffer, sizeof(nameBuffer), kCFStringEncodingUTF8)) {
                                ret <<  nameBuffer << ",";
                            } else {
                                ret << "Failed to convert application name to C string." << "\n";
                                errors++;
                            }
                        } else {
                            ret << "Application name not found in Info.plist." << "\n";
                            errors++;
                        }
                        
                        ret <<  path << "," << iconPath << "\n";
                        
                        CFRelease(bundle);
                    } else {
                        ret << "Failed to create bundle from URL." << "\n";
                    }
            }

            CFRelease(appURLs);
            
        }
        
    }
    ret << "Exit ext=" << name << " errors="<< errors << "\n";
    std::string s = ret.str();
    
    return env->NewStringUTF(s.data());
}
