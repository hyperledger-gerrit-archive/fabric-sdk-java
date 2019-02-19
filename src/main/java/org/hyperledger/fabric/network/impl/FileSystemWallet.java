/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.network.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;

import org.apache.commons.io.FileUtils;
import org.hyperledger.fabric.network.Wallet;
import org.hyperledger.fabric.sdk.security.CryptoPrimitives;

public class FileSystemWallet implements Wallet {
  private Path basePath;

  public FileSystemWallet(Path path) throws IOException {
    boolean walletExists = Files.exists(path);
    if (!walletExists) {
        Files.createDirectories(path);
    }
    basePath = path;
  }

  @Override
  public void put(String label, Identity identity) throws Exception {
    Path idFolder = basePath.resolve(label);
    if (!Files.exists(idFolder)) {
      Files.createDirectories(idFolder);
    }
    Path idFile = basePath.resolve(Paths.get(label, label));
    try (Writer fw = Files.newBufferedWriter(idFile)) {
      String json = toJson(identity);
      fw.append(json);
    }

    Path pemFile = basePath.resolve(Paths.get(label, label + "-priv"));
    writePrivateKey(identity.getEnrollment().getKey(), pemFile);
  }

  @Override
  public Identity get(String label) throws Exception {
    Path idFile = basePath.resolve(Paths.get(label, label));
    if (Files.exists(idFile)) {
      try (BufferedReader fr = Files.newBufferedReader(idFile)) {
        String contents = fr.readLine();
        return fromJson(contents);
      }
    }
    return null;
  }

  @Override
  public Set<String> getAllLabels() {
    List<File> files = Arrays.asList(basePath.toFile().listFiles(File::isDirectory));
    Set<String> labels = files.stream().map(file -> file.getName()).collect(Collectors.toSet());
    return labels;
  }

  @Override
  public void remove(String label) throws IOException {
    Path idDir = basePath.resolve(label);
    if (Files.exists(idDir)) {
      FileUtils.deleteDirectory(idDir.toFile());
    }
  }

  @Override
  public boolean exists(String label) {
    Path idFile = basePath.resolve(Paths.get(label, label));
    return Files.exists(idFile);
  }

  Identity fromJson(String json) throws Exception {
    try (JsonReader reader = Json.createReader(new StringReader(json))) {
      JsonObject idObject = reader.readObject();
      String name = idObject.getString("name");  // TODO assert this is the same as the folder
      String mspId = idObject.getString("mspid");
      JsonObject enrollment = idObject.getJsonObject("enrollment");
      String signingId = enrollment.getString("signingIdentity");
      Path pemFile = basePath.resolve(Paths.get(name, signingId + "-priv"));
      PrivateKey privateKey = readPrivateKey(pemFile);
      String certificate = enrollment.getJsonObject("identity").getString("certificate");
      return new WalletIdentity(name, mspId, certificate, privateKey);
    }
  }

  static String toJson(Identity identity) {
    String json = null;
    JsonObject idObject = Json.createObjectBuilder()
        .add("name", identity.getName())
        .add("type", "X509")
        .add("mspid", identity.getMspId())
        .add("enrollment", Json.createObjectBuilder()
            .add("signingIdentity", identity.getName())
            .add("identity", Json.createObjectBuilder()
                .add("certificate", identity.getEnrollment().getCert())))
        .build();

    StringWriter writer = new StringWriter();
    try (JsonWriter jw = Json.createWriter(writer)) {
        jw.writeObject(idObject);
    }
    json = writer.toString();
    return json;
  }

  static PrivateKey readPrivateKey(Path pemFile) throws Exception {
    if (Files.exists(pemFile)) {
      try (BufferedReader reader = Files.newBufferedReader(pemFile)) {
        StringBuilder encoded = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
          encoded.append(line);
          encoded.append('\n');
          line = reader.readLine();
        }
        CryptoPrimitives cp = new CryptoPrimitives();
        return cp.bytesToPrivateKey(encoded.toString().getBytes());
      }
    }
    return null;
  }

  static void writePrivateKey(PrivateKey key, Path pemFile) throws Exception {
    try (BufferedWriter writer = Files.newBufferedWriter(pemFile)) {
      writer.append(new String(key.getEncoded(), "UTF-8"));
    }
  }

}
