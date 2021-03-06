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
package org.apache.accumulo.examples.client;

import java.util.Map.Entry;

import org.apache.accumulo.core.client.Accumulo;
import org.apache.accumulo.core.client.AccumuloClient;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.examples.Common;
import org.apache.accumulo.examples.cli.ClientOpts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A demonstration of reading entire rows and deleting entire rows.
 */
public final class RowOperations {

  private static final Logger log = LoggerFactory.getLogger(RowOperations.class);

  static final String ROWOPS_TABLE = Common.NAMESPACE + ".rowops";

  private RowOperations() {}

  private static void printAll(AccumuloClient client) throws TableNotFoundException {
    try (Scanner scanner = client.createScanner(ROWOPS_TABLE, Authorizations.EMPTY)) {
      for (Entry<Key,Value> entry : scanner) {
        log.info("Key: {} Value: {}", entry.getKey().toString(), entry.getValue().toString());
      }
    }
  }

  private static void printRow(String row, AccumuloClient client) throws TableNotFoundException {
    try (Scanner scanner = client.createScanner(ROWOPS_TABLE, Authorizations.EMPTY)) {
      scanner.setRange(Range.exact(row));
      for (Entry<Key,Value> entry : scanner) {
        log.info("Key: {} Value: {}", entry.getKey().toString(), entry.getValue().toString());
      }
    }
  }

  private static void deleteRow(String row, AccumuloClient client, BatchWriter bw)
      throws MutationsRejectedException, TableNotFoundException {
    Mutation mut = new Mutation(row);
    try (Scanner scanner = client.createScanner(ROWOPS_TABLE, Authorizations.EMPTY)) {
      scanner.setRange(Range.exact(row));
      for (Entry<Key,Value> entry : scanner) {
        mut.putDelete(entry.getKey().getColumnFamily(), entry.getKey().getColumnQualifier());
      }
    }
    bw.addMutation(mut);
    bw.flush();
  }

  public static void main(String[] args)
      throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
    ClientOpts opts = new ClientOpts();
    opts.parseArgs(RowOperations.class.getName(), args);

    try (AccumuloClient client = Accumulo.newClient().from(opts.getClientPropsPath()).build()) {
      Common.createTableWithNamespace(client, ROWOPS_TABLE);

      // lets create 3 rows of information
      Mutation mut1 = new Mutation("row1");
      Mutation mut2 = new Mutation("row2");
      Mutation mut3 = new Mutation("row3");

      mut1.put("col", "1", "v1");
      mut1.put("col", "2", "v2");
      mut1.put("col", "3", "v3");

      mut2.put("col", "1", "v1");
      mut2.put("col", "2", "v2");
      mut2.put("col", "3", "v3");

      mut3.put("col", "1", "v1");
      mut3.put("col", "2", "v2");
      mut3.put("col", "3", "v3");

      // Now we'll make a Batch Writer
      try (BatchWriter bw = client.createBatchWriter(ROWOPS_TABLE)) {

        // And add the mutations
        bw.addMutation(mut1);
        bw.addMutation(mut2);
        bw.addMutation(mut3);

        // Force a send
        bw.flush();

        log.info("This is only row2");
        printRow("row2", client);

        log.info("This is everything");
        printAll(client);

        deleteRow("row2", client, bw);

        log.info("This is row1 and row3");
        printAll(client);

        deleteRow("row1", client, bw);
      }

      log.info("This is just row3");
      printAll(client);

      client.tableOperations().delete(ROWOPS_TABLE);
    }
  }
}
