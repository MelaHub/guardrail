package com.twilio.guardrail
package generators

import cats.~>
import cats.arrow.FunctionK
import Http4sClientGenerator._
import AkkaHttpServerGenerator._
import Http4sGenerator._
import com.twilio.guardrail.generators.circe.protocol.CirceProtocolGenerator._
import ScalaGenerator._
import SwaggerGenerator._
import com.twilio.guardrail.generators.circe.protocol.ProtocolSupportTermInterp

object Http4s extends FunctionK[CodegenApplication, Target] {
  val interpSP: CodegenApplicationSP ~> Target                 = ProtocolSupportTermInterp or ServerTermInterp
  val interpMSP: CodegenApplicationMSP ~> Target               = ModelProtocolTermInterp or interpSP
  val interpEMSP: CodegenApplicationEMSP ~> Target             = EnumProtocolTermInterp or interpMSP
  val interpCEMSP: CodegenApplicationCEMSP ~> Target           = ClientTermInterp or interpEMSP
  val interpACEMSP: CodegenApplicationACEMSP ~> Target         = AliasProtocolTermInterp or interpCEMSP
  val interpACEMSSP: CodegenApplicationACEMSSP ~> Target       = ScalaInterp or interpACEMSP
  val interpACEMSSPR: CodegenApplicationACEMSSPR ~> Target     = ArrayProtocolTermInterp or interpACEMSSP
  val interpACEMSSPRS: CodegenApplicationACEMSSPRS ~> Target   = SwaggerInterp or interpACEMSSPR
  val interpACEMSSPRSF: CodegenApplicationACEMSSPRSF ~> Target = FrameworkInterp or interpACEMSSPRS
  def apply[T](x: CodegenApplication[T]): Target[T]            = interpACEMSSPRSF.apply(x)
}
