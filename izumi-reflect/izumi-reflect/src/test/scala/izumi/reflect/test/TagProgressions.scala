package izumi.reflect.test

import org.scalatest.exceptions.TestFailedException

trait TagProgressions { this: TagAssertions =>

  final def doesntWorkYetOnDotty(f: => Any): Unit = {
    if (IsDotty) doesntWorkYet(f) else f; ()
  }
  final def doesntWorkYetOnScala2(f: => Any): Unit = {
    if (!IsDotty) doesntWorkYet(f) else f; ()
  }
  final def doesntWorkYet(f: => Any): Unit = {
    intercept[TestFailedException](f); ()
  }

  final def succeedsOnDottyButShouldnt(f: => Any): Unit = {
    doesntWorkYetOnScala2(f)
  }
  final def succeedsOnScala2ButShouldnt(f: => Any): Unit = {
    doesntWorkYetOnDotty(f)
  }

}
