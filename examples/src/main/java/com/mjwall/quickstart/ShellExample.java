/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mjwall.quickstart;

import com.google.common.io.Files;

import org.apache.accumulo.minicluster.MiniAccumuloCluster;
import org.apache.accumulo.core.util.shell.Shell;

import java.io.File;
import java.io.IOException;
import java.lang.InterruptedException;
import java.lang.Runnable;

public class ShellExample implements Runnable {

  @Override
  public void run() {
    File tempDir = null;
    MiniAccumuloCluster mac = null;

    try {
      tempDir = Files.createTempDir();

      final String PASSWORD = "pass1234";

      mac = new MiniAccumuloCluster(tempDir, PASSWORD);
      System.out.println("Starting the MiniAccumuloCluster in " + tempDir.getAbsolutePath());
      mac.start();

      String[] args = new String[] {"-u", "root", "-p", PASSWORD, "-z",
        mac.getInstanceName(), mac.getZooKeepers()};

      Shell.main(args);

    } catch (InterruptedException e) {
      System.err.println("Error starting MiniAccumuloCluster: " + e.getMessage());
      System.exit(1);
    } catch (IOException e) {
      System.err.println("Error starting MiniAccumuloCluster: " + e.getMessage());
      System.exit(1);
    } finally {
        if (null != tempDir) {
            tempDir.delete();
        }
        if (null != mac) {
            try {
                mac.stop();
            } catch (InterruptedException e) {
                System.err.println("Error stopping MiniAccumuloCluster: " + e.getMessage());
                System.exit(1);
            } catch (IOException e) {
                System.err.println("Error stopping MiniAccumuloCluster: " + e.getMessage());
                System.exit(1);
            }
        }
    }
  }

  public static void main(String[] args) {
    System.out.println("\n   ---- Initializing Accumulo Shell\n");

    ShellExample shell = new ShellExample();
    shell.run();

    System.exit(0);
  }
}
