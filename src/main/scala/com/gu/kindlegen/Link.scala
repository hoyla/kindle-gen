package com.gu.kindlegen

import java.net.{URI, URL}
import java.nio.file.{Files, Path, Paths}

import scala.util.Try


sealed trait Link {
  /** The link to use to reach this item.
    *
    * The link may be a URL, a Path, a Data URI, etc.
    */
  def source: String
}

object Link {
  sealed case class AbsoluteURL  protected[Link](source: String) extends Link
  final       class DataURI      protected[Link](source: String) extends AbsoluteURL(source)

  final case class  AbsolutePath protected[Link](source: String) extends Link {
    def toPath: Path = Paths.get(source)
    def isEquivalentTo(that: AbsolutePath): Boolean = Files.isSameFile(this.toPath, that.toPath)
  }

  final case class  RelativePath protected[Link](source: String, relativeTo: AbsolutePath) extends Link {
    def toAbsolutePath: AbsolutePath = AbsolutePath.from(relativeTo.toPath.resolve(source))
  }


  object AbsoluteURL extends Factory(new AbsoluteURL(_), new URL(_)) {
    protected override def validate(source: URL): Unit = {
      require(source.toURI.isAbsolute, "URL must be absolute")
    }
  }

  object DataURI extends Factory(new DataURI(_), new URI(_)) {
    protected override def validate(source: URI): Unit = {
      require(source.getScheme == "data", "URI must be start with 'data:'")
    }
  }

  object AbsolutePath extends Factory(str => new AbsolutePath(Paths.get(str).toRealPath().toString), Paths.get(_)) {
    protected override def validate(source: Path): Unit =
      require(source.isAbsolute, "Path must be absolute")
  }

  object RelativePath {
    def apply(source: Path, relativeTo: AbsolutePath): RelativePath =
      new RelativePath(source.toString, relativeTo)

    def apply(source: String, relativeTo: AbsolutePath): Try[RelativePath] =
      Try(Paths.get(source)).map(_ => new RelativePath(source, relativeTo))

    def from(source: Path, relativeTo: AbsolutePath): RelativePath = apply(source, relativeTo)
    def from(source: String, relativeTo: AbsolutePath): RelativePath = apply(source, relativeTo).get
  }

  abstract class Factory[T <: Link, Mirror](constructor: String => T, mirrorConstructor: String => Mirror) {
    def apply(source: Mirror): Try[T] = Try(validate(source)).map(_ => constructor(source.toString))
    def apply(source: String): Try[T] = Try(validate(mirrorConstructor(source))).map(_ => constructor(source))

    def from(source: Mirror): T = apply(source).get
    def from(source: String): T = apply(source).get

    protected def validate(source: Mirror): Unit
  }
}
