# **Servicio streaming que sea capaz de leer un dato de otro emisor streaming, realizar una evalaución en algun motor de reglas o un cruce con base de datos en memoría y posterior emisión de dato a la red bajo patron publish-suscribe.**

**Integrantes:** <br />
Cristóbal Becerra <br />
Leiser Mahu<br />
Nicolás Olivares <br />

## 1. Descripción del problema:

El streaming consiste en la transmisión de datos secuenciales entre un emisor y un receptor, estos datos pueden ser analizados y transformados en información a medida que van llegando, difiriendo así del procesamiento de datos por lotes.
El problema consiste en implementar una solución en Google Cloud capaz de recibir datos de una plataforma streaming, en este caso se eligió Twitter mediante su API para desarrolladores y realizar la publicación de estos datos mediante mensajes con el patrón Publish Subscribe luego de realizar algún procesamiento de estos en memoria, para cumplir esto se implementará un filtro para procesar solo los datos que contengan ciertas palabras clave.

## 2. Enfoque de solución

El enfoque que se dará a la solución consiste en dos módulos distintos, una aplicación Java dedicada a establecer el pipeline de streaming de datos en Google Dataflow capturando mensajes publicados por Google Pub/Sub y un script Python dedicado a capturar tweets desde la API de Twitter Developers con tweepy para publicarlos mediante Google Pub/Sub.

## 3. Desarrollo de la acividad

Tecnologas utilizadas:

- Python 2.7 <br />
- API Twitter Developers <br />
- Tweepy <br />
- Google Cloud Platforms <br />
- Google DataFlow <br />
- Google BigQuery <br />


### Principales inconvenientes o barreras detectadas

La siguiente lista representa aspectos que se distinguieron como barreras y limitaciones en la implementación de la solución:

- Falta de conocimiento: El escaso conocimiento que teníamos sobre la plataforma de Google Cloud significó un proceso de aprendizaje autónomo. Tuvimos que recurrir a tutoriales, demos y repositorios de otros autores para fundar nuestra solución en herramientas que entendiéramos.

- Problemas de configuración: Debido a no estar familiarizados con la plataforma de Google Cloud, debimos dilucidar en qué secciones estaban las principales funcionalidades utilizadas para verificar que se estuviera realizando las actividades del pipeline de streaming de datos.

- Despliegue de aplicaciones y arquitectura: Al momento de iniciar esta experiencia no teníamos conocimientos sobre como montar arquitecturas y aplicaciones en servicios cloud desde código o archivos de fácil despliegue, para esto se tuvo que investigar a fondo el funcionamiento del SDK de Google Cloud, la sintaxis de los archivos a utilizar (.yaml en este caso) y su posterior administración una vez montados.
 
- Permisos: Uno de los aspectos más problemáticos fue el proceso de conceder permisos a los distintos recursos de plataformas cloud, el proceso de montaje y despliegue muchas veces depende de cuentas de servicio generadas automáticamente al construir la arquitectura que no contaban con los permisos necesarios, se tuvo que invertir gran cantidad de tiempo en investigación acerca de qué permisos eran necesarios para llevar a cabo las tareas y cómo asignarlos correctamente.

### Clases/Funciones/Procedimientos principales del desarrollo

#### Clases en Java:
- TweetPipeline: El objetivo de esta clase es establecer el pipeline para leer los tweets desde un tópico de Google PubSub y escribirlos en BigQuery, el cual es un data warehouse en la cual se almacenan los tweets, en específico, su payload o carga útil, que es donde se almacenan las variables que describen el tweet y su timestamp, que corresponde a su fecha de creación.
El tópico y el destino de BigQuery son estáticos y están en el repositorio de este proyecto.

#### Funciones en Python:

 - Captura de tweets: Utilizando tweepy, una librería de Python que accede a la API de Twitter, se capturan los tweets. Esta necesita unas credenciales de google para autentificarse y para acceder al servicio de pubsub que se usará en la función de publish. Existe un filtrado de tweets en la captura de tweets que corresponde a que sean en español y que tengan como palabras clave: chile, usach, google y cloud.

 - Publish: Este método hace uso del método del patrón publisher-subscriber en Google Pub/Sub y publica un número finito de tweets ya predefinido dentro del tópico estático el cual está almacenado en BigQuery. 


## 4. Resultados

![Alt text](https://i.imgur.com/KODkS3w.png "Pipeline generado en Dataflow")

![Alt text](https://i.imgur.com/Saz1BIo.png "Pipeline generado extendido en Dataflow")

![Alt text](https://i.imgur.com/4b95bHG.png "Resultados en BigQuery")

![Alt text](https://i.imgur.com/uRqbTRl.png "Resultados en BigQuery formateados")




## 5. Repositorios

Se puede encontrar la versión productiva del software en: https://github.com/neolivaresgonzalez/gdataflow_streaming_pub_sub_py

## 6. Deployment

Antes de realizar el deployment es necesario contar con algunos recursos en el proyecto en google cloud:
 
- Se requiere tener activado Google Pub/Sub con un tema llamado twitter, en caso de utilizar otro nombre se debe cambiar la dirección del tema en el script Python.

- Se requiere tener activado Google Bigquery con una base de datos de nombre twitter y una tabla tweets, en caso de querer utilizar otros nombres se deben actualizar en la clase principal Tweetpipeline. La tabla tweets debe tener dos campos:
  - timestamp, con tipo Integer.
  - payload, con tipo String.

- Se debe tener activada la API de Google App Engine.

- Una vez satisfechos los requerimientos anteriores solo necesita clonar este repositorio, ir a la raíz y ejecutar el siguiente comando:

  - gcloud builds submit --config=cloudbuild.yaml




