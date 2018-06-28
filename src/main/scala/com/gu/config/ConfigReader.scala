package com.gu.config

import scala.util.Try

import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader


object ConfigReader {
  def apply[T: ValueReader](parentConfigurationPath: String) = new ConfigReader[T] {
    def apply(config: Config): Try[T] = Try(config.as[T])
    protected override def parentConfigPath: String = parentConfigurationPath
  }

  def root[T: ValueReader]: RootConfigReader[T] = config => Try(config.as[T])
}

trait ConfigReader[T] {
  def fromParentConfig(root: Config): Try[T] =
    Try(root.getConfig(parentConfigPath)).flatMap(apply)

  def apply(config: Config): Try[T]

  protected def parentConfigPath: String
}

trait RootConfigReader[T] extends ConfigReader[T] {
  def load: Try[T] = apply(ConfigFactory.load)
  override def fromParentConfig(root: Config): Try[T] = apply(root)
  protected final override def parentConfigPath: String = throw new UnsupportedOperationException
}
