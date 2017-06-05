# Demonstration of an alternative Prometheus API:

* Lexical compatibility with Codahale/Dropwizard API for Barclays/Insurance, simplifying complete migration.
* Simpler, cleaner timer / histogram syntax, via resource/try
* Name-based metrics; no need to create instances or handle registration.
* Aim is to simplify Prometheus adoption, reduce excessive code intrusion.
* Prometheus API can be used directly for more advanced cases (hopefully not necessary)