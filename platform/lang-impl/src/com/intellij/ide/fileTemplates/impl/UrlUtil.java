/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ide.fileTemplates.impl;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.io.URLUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Eugene Zhuravlev
 *         Date: 3/25/11
 */
class UrlUtil {
  private static final String JAR_SEPARATOR = "!/";
  private static final String URL_PATH_SEPARATOR = "/";
  private static final String FILE_PROTOCOL = "file";
  private static final String FILE_PROTOCOL_PREFIX = FILE_PROTOCOL + ":";
  private static final String JAR_PROTOCOL = "jar";
  private static final String JAR_PROTOCOL_PREFIX = JAR_PROTOCOL + ":";

  public static String loadText(URL url) throws IOException {
    final InputStream stream = new BufferedInputStream(URLUtil.openStream(url));
    try {
      return new String(FileUtil.loadBytes(stream), FileTemplate.ourEncoding);
    }
    finally {
      stream.close();
    }
  }

  public static List<String> getChildrenRelativePaths(URL root) throws IOException {
    final String protocol = root.getProtocol();
    if ("jar".equalsIgnoreCase(protocol)) {
      return getChildPathsFromJar(root);
    }
    if ("file".equalsIgnoreCase(protocol)){
      return getChildPathsFromFile(root);
    }
    return Collections.emptyList();
  }

  private static List<String> getChildPathsFromFile(URL root) {
    final List<String> paths = new ArrayList<String>();
    final File rootFile = new File(root.getPath());
    new Object() {
      void collectFiles(File fromFile, String prefix) {
        final File[] list = fromFile.listFiles();
        if (list != null) {
          for (File file : list) {
            final String childRelativePath = prefix.length() == 0 ? file.getName() : prefix + URL_PATH_SEPARATOR + file.getName();
            if (file.isDirectory()) {
              collectFiles(file, childRelativePath);
            }
            else {
              paths.add(childRelativePath);
            }
          }
        }
      }
    }.collectFiles(rootFile, "");
    return paths;
  }

  private static List<String> getChildPathsFromJar(URL root) throws IOException {
    final List<String> paths = new ArrayList<String>();
    String file = root.getFile();
    if (file.startsWith(FILE_PROTOCOL_PREFIX)) {
      file = file.substring(FILE_PROTOCOL_PREFIX.length());
    }
    final int jarSeparatorIndex = file.indexOf(JAR_SEPARATOR);
    assert jarSeparatorIndex > 0;

    String rootDirName = file.substring(jarSeparatorIndex + 2);
    if (!rootDirName.endsWith(URL_PATH_SEPARATOR)) {
      rootDirName += URL_PATH_SEPARATOR;
    }
    final ZipFile zipFile = new ZipFile(FileUtil.unquote(file.substring(0, jarSeparatorIndex)));
    try {
      final Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        final ZipEntry entry = entries.nextElement();
        if (!entry.isDirectory()) {
          final String relPath = entry.getName();
          if (relPath.startsWith(rootDirName)) {
            paths.add(relPath.substring(rootDirName.length()));
          }
        }
      }
      return paths;
    }
    finally {
      zipFile.close();
    }
  }
}
