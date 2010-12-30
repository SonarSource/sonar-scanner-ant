/*
 * Sonar Ant Task
 * Copyright (C) 2009 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ant;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Copied from Sonar mojo with modifications.
 */
public class ServerMetadata {

  public static final int CONNECT_TIMEOUT_MILLISECONDS = 30000;
  public static final int READ_TIMEOUT_MILLISECONDS = 60000;

  private String url;
  private String version;

  public ServerMetadata(String url) {
    if (url.endsWith("/")) {
      this.url = url.substring(0, url.length() - 1);
    } else {
      this.url = url;
    }
  }

  public String getVersion() throws IOException {
    if (version == null) {
      version = remoteContent("/api/server/version");
    }
    return version;
  }

  public String getUrl() {
    return url;
  }

  protected String remoteContent(String path) throws IOException {
    String fullUrl = url + path;
    HttpURLConnection conn = getConnection(fullUrl + path, "GET");
    Reader reader = new InputStreamReader((InputStream) conn.getContent());
    try {
      int statusCode = conn.getResponseCode();
      if (statusCode != HttpURLConnection.HTTP_OK) {
        throw new IOException("Status returned by url : '" + fullUrl + "' is invalid : " + statusCode);
      }
      return IOUtils.toString(reader);

    } finally {
      IOUtils.closeQuietly(reader);
      conn.disconnect();
    }
  }

  static HttpURLConnection getConnection(String url, String method) throws IOException {
    URL page = new URL(url);
    HttpURLConnection conn = (HttpURLConnection) page.openConnection();
    conn.setConnectTimeout(CONNECT_TIMEOUT_MILLISECONDS);
    conn.setReadTimeout(READ_TIMEOUT_MILLISECONDS);
    conn.setRequestMethod(method);
    conn.connect();
    return conn;
  }

  protected boolean supportsAnt() throws IOException {
    // TODO
    return true;
  }
}
