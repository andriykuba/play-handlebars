package aku.play.handlebars;

import play.api.Configuration;
import play.api.Environment;
import play.api.inject.Binding;
import scala.collection.Seq;

public class HandlebarsModule extends play.api.inject.Module {

  @Override
  public Seq<Binding<?>> bindings(final Environment environment, final Configuration configuration) {
    return seq(bind(HandlebarsApi.class).toSelf());
  }

}
