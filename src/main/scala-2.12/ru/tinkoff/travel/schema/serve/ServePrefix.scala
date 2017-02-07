package ru.tinkoff.travel.schema.serve

import akka.http.scaladsl.server._
import Directives._
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import ru.tinkoff.travel.schema.Name
import ru.tinkoff.travel.schema.typeDSL._
import shapeless.ops.hlist._
import shapeless.{::, HList, HNil, Witness}

trait ServePrefix[T, I <: HList] extends ServePartial[T, I] {
  def handle(f: (I) ⇒ Route): Route
}

object ServePrefix {
  private def prefixServe[pref, T](implicit name: Name[pref]): ServePrefix[T, HNil] =
    f => pathPrefix(name.string)(f(HNil))

  implicit def prefixWrapServe[pref: Name] = prefixServe[pref, Prefix[pref]]

  implicit def prefixStrServe[pref: Name] = prefixServe[pref, pref]

  implicit def prefixWitnessServe[pref: Name] = prefixServe[pref, Witness.Aux[pref]]

  implicit def queryParamServe[name, x]
  (implicit fromQueryParam: FromQueryParam[x], name: Name[name]): ServePrefix[QueryParam[name, x], x :: HNil] =
    f => parameter(name.symbol)(param ⇒ f(fromQueryParam(param) :: HNil))

  implicit def queryOptParamServe[name, x]
  (implicit fromQueryParam: FromQueryParam[x], name: Name[name]): ServePrefix[QueryParam[name, Option[x]], Option[x] :: HNil] =
    f => parameter(name.symbol.?)(param ⇒ f(param.map(fromQueryParam(_)) :: HNil))

  implicit def queryParamsServe[name, x]
  (implicit fromQueryParam: FromQueryParam[x], name: Name[name]): ServePrefix[QueryParams[name, x], List[x] :: HNil] =
    f => parameterMultiMap(paramMap ⇒ f(paramMap.getOrElse(name.string, Nil).map(fromQueryParam(_)) :: HNil))

  implicit def queryFlagServe[name, x]
  (implicit name: Name[name]): ServePrefix[QueryFlag[name], Boolean :: HNil] =
    f => parameterMap(paramMap ⇒ f((paramMap contains name.string) :: HNil))

  implicit def captureServe[name: Name, x]
  (implicit fromPathParam: FromPathParam[x]): ServePrefix[Capture[name, x], x :: HNil] =
    f => pathPrefix(fromPathParam.matcher)(x ⇒ f(x :: HNil))

  implicit def reqBodyServe[x: FromRequestUnmarshaller]: ServePrefix[ReqBody[x], x :: HNil] =
    f => entity(as[x])((x: x) ⇒ f(x :: HNil))

  implicit def headerServe[name: Name, x]
  (implicit fromHeader: FromHeader[x]): ServePrefix[Header[name, x], x :: HNil] =
    f => headerValue(fromHeader(_))(x ⇒ f(x :: HNil))

  implicit def formFieldServe[name, x]
  (implicit fromFieldParam: FromFormField[x], name: Name[name]): ServePrefix[FormField[name, x], x :: HNil] =
    f => formField(name.symbol)(field ⇒ f(fromFieldParam(field) :: HNil))

  implicit def cookieServe[name, x]
  (implicit fromCookie: FromCookie[x], name: Name[name]): ServePrefix[Cookie[name, x], x :: HNil] =
    f => cookie(name.string)(cook ⇒ f(fromCookie(cook.value) :: HNil))

  implicit def consServe[start, end, I1 <: HList, I2 <: HList]
  (implicit start: ServePrefix[start, I1], end: ServePrefix[end, I2], prepend: Prepend[I1, I2]): ServePrefix[start :> end, prepend.Out] =
    f => start.handle(i1 ⇒ end.handle(i2 ⇒ f(prepend(i1, i2))))

  implicit def metaServe[x <: Meta]: ServePrefix[x, HNil] = f => f(HNil)
}




