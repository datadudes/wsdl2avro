WSDL2Avro [![Build Status](https://travis-ci.org/datadudes/wsdl2avro.svg?branch=master)](https://travis-ci.org/datadudes/wsdl2avro)
=========

WSDL2Avro is a Scala library that lets you convert datatypes from a SOAP WSDL to Avro Schemas. It takes the XML types as 
defined in the `<types>` section of a WSDL document and converts them into Avro Schema objects that can then be used 
programmatically or saved to disk. This library is particulary useful when you want to consume a SOAP service, and store 
the data somewhere (e.g. in a Hadoop cluster) serialized in Avro format.

This library is very much alpha quality for now.

Features:

- Provide a path to a WSDL document, or a File object, and get a `Map` with type names and Avro `Schema` objects in return.
- Converts XML primitives to Avro primitives
- Takes care of inheritance (which Avro doesn't support) by keeping track of base types and combining the fields of 
base types with those of inherited types.

## Installation

_WSDL2Avro_ is published on Maven Central, so it's easy to include it in your project:

#### SBT

```scala
libraryDependencies += "co.datadudes" %% "wsdl2avro" % "0.2.1"
```

#### Maven

```scala
<dependency>
  <groupId>co.datadudes</groupId>
  <artifactId>wsdl2avro_2.10</artifactId>
  <version>0.2.1</version>
</dependency>
```

Currently, wsdl2avro is being cross-built for Scala 2.10.4 and 2.11.4, so use the appropriate suffex in Maven.

If you want to be on the bleeding edge, just clone this repo and publish the library to your local ivy/maven repository 
using `sbt publish-local` or `sbt publish-m2`, and then include it in your sbt dependencies like this:

```scala
libraryDependencies += "co.datadudes" %% "wsdl2avro" % "0.3-SNAPSHOT"
```

or in your maven dependencies like this:

```xml
<dependency>
  <groupId>co.datadudes</groupId>
  <artifactId>wsdl2avro_2.10</artifactId>
  <version>0.3-SNAPSHOT</version>
</dependency>
```

You can run tests with `sbt test`.

## Usage

Using this library is really easy:

```scala
scala> import co.datadudes.wsdl2avro.WSDL2Avro._
import co.datadudes.wsdl2avro.WSDL2Avro._

scala> val schemas = convert("/path/to/some.wsdl")
schemas: Map[String,org.apache.avro.Schema] = Map(ListViewRecord -> {"type":"record","name":"ListViewRecord","fields":[{"name":"columns","type":"string"}]}, Scontrol -> {"type":"record","name":"Scontrol","fields":[{"name":"fieldsToNull","type":"string"},{"name":"Id","type":"string"},{"name":"Binary","type":"string"},{"name":"BodyLength","type":"int"},{"name":"ContentSource","type":"string"},{"name":"CreatedBy","type":"string"},{"name":"CreatedById","type":"string"},{"name":"CreatedDate","type":"string"},{"name":"Description","type":"string"},{"name":"DeveloperName","type":"string"},{"name":"EncodingKey","type":"string"},{"name":"Filename","type":"string"},{"name":"HtmlWrapper","type":"string"},{"name":"LastModifiedBy","type":"string"},{"name":"LastModifiedById","type":"string"},{"name...
```

You can also pretty-print the Avro Schema, which is useful for writing it to a file:

```scala
schemas.toString(true)
```

You can call some of the steps in the conversion process manually:

**Get the type definitions from the WSDL** _(returns a sequence of `Node` objects)_

```scala
WSDL2Avro.getDataTypeDefinitions(wsdl: Elem)
```

**Convert a single type definition (from your WSDL) to an Avro Schema** _(returns an Avro `Schema` object)_

```scala
WSDL2Avro.xmlType2Schema(node: Node, parentNode: Option[Node] = None)
```

If the Node is a `complexType` that inherits from another `complexType`, you can provide the parent as well. Because 
Avro doesn't support inheritance, it will combine the fields of parent and child into one Record Schema.

**Convert one `<element>` from a complexType to an Avro field** _(returns an Avro `Field` object)_

```scala
WSDL2Avro.element2field(node: Node)
```

**Convert a list of type definitions to a list of Avro Schemas** _(returns a `Seq` of Avro `Schema` objects)_

This will automatically recognize inheritance and provide the proper parent `complexType` when converting the child.

```scala
WSDL2Avro.createSchemasFromXMLTypes(typeList: Seq[Node])
```

## Background

This library was created as part of an effort to build a tool that allows ingesting data from Salesforce using their 
SOAP API, and putting it into Hadoop/HDFS in Avro serialized format. In order to convert the XML from the Salesforce API 
to Avro, we need to have proper Avro Schemas for all types of objects we want to store. This is how _wsdl2avro_ was born.

The Salesforce->Hadoop tool [can be found here](https://github.com/datadudes/salesforce2hadoop)

#### Type conversion

_wsdl2avro_ does its best to convert primitives as defined in the XSD spec, to corresponding Avro types, but in some cases, 
where no direct mapping exists, a suitable alternative was chosen. Complex types with fields that reference another complex 
type, have `string` as type in the corresponding Avro Schema. This is mainly because for our specific use case, we want 
to store data we consume from a SOAP API in a flat (non-nested) manner, and refer to related objects by id. If there's 
enough demand, we will also add support for referencing other record types in the resulting Avro.

## Contributing

You're more than welcome to create issues for any bugs you find and ideas you have. Contributions in the form of pull 
requests are also very much appreciated!

## Authors

wsdl2avro was created with passion by:

- [Daan Debie](https://github.com/DandyDev) - [Website](http://dandydev.net/)
- [Marcel Krcah](https://github.com/mkrcah) - [Website](http://marcelkrcah.net/)