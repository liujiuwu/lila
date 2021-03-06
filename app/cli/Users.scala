package lila
package cli

import lila.user.{ UserEnv, User, UserRepo }
import lila.security.{ Store, Permission }
import scalaz.effects._

private[cli] case class Users(userEnv: UserEnv, deleteUsername: String ⇒ IO[Unit]) {

  private def userRepo = userEnv.userRepo

  def rewriteHistory: IO[String] =
    userEnv.historyRepo.fixAll inject "History rewritten"

  def roles(username: String): IO[String] = for {
    userOption ← userRepo byId username
  } yield userOption.fold(_.roles mkString " ", "User not found")

  def grant(username: String, roles: List[String]): IO[String] =
    perform(username, { user ⇒
      userRepo.setRoles(user, roles map (_.toUpperCase))
    })

  def enable(username: String): IO[String] =
    perform(username, userRepo.enable)

  def disable(username: String): IO[String] =
    perform(username, user ⇒
      userRepo disable user map { _ ⇒
        deleteUsername(username)
      }
    )

  def passwd(username: String, password: String) =
    perform(username, user ⇒ {
      userRepo.passwd(user, password) map (_.fold(
        errors ⇒ throw new RuntimeException(errors.shows),
        _ ⇒ io()
      ))
    })

  private def perform(username: String, op: User ⇒ IO[Unit]): IO[String] = for {
    userOption ← userRepo byId username
    res ← userOption.fold(
      u ⇒ op(u) inject "Success",
      io("User not found")
    )
  } yield res
}
