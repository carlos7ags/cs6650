package com.cs6650.datapublisher;

public abstract class RedisDataPublisher implements RedisDataPublisherInterface {

  protected String addRecord(String listOfRecords, String record) {
    return listOfRecords.substring(0, listOfRecords.length() - 1) + ", " + record + "]";
  }

}
