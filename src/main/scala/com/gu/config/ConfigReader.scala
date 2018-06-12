package com.gu.config

import scala.util.Try

import com.typesafe.config.{Config, ConfigFactory}

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

abstract class AbstractConfigReader[T](protected override val parentConfigPath: String) extends ConfigReader[T]
