package aku.play.handlebars;

import play.twirl.api.Content;

class HtmlContent implements Content {

  private String body;
  
  HtmlContent(final String body){
    this.body = body;
  }
  
  @Override
  public String body() {
    return body;
  }

  @Override
  public String contentType() {
    return "text/html";
  }

}
