package com.arize.types;

import java.util.List;

public class Embedding {

  private List<Double> vector;

  private List<String> rawData;

  private String linkToData;

  public Embedding(List<Double> vector, List<String> rawData, String linkToData) {
    this.vector = vector;
    this.rawData = rawData;
    this.linkToData = linkToData;
  }

  public List<Double> getVector() {
    return vector;
  }

  public void setVector(List<Double> vector) {
    this.vector = vector;
  }

  public List<String> getRawData() {
    return rawData;
  }

  public void setRawData(List<String> rawData) {
    this.rawData = rawData;
  }

  public String getLinkToData() {
    return linkToData;
  }

  public void setLinkToData(String linkToData) {
    this.linkToData = linkToData;
  }
}
