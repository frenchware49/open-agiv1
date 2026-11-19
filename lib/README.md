# lib/

Bu klasör harici JAR dosyaları içindir.

## Weka

`build-weka.bat` için `weka.jar` dosyası gerekir:

1. https://sourceforge.net/projects/weka/files/weka-3-8/3.8.7/ adresinden `weka-3-8-7.zip` indirin
2. ZIP'ten `weka.jar` dosyasını çıkarıp buraya `weka.jar` olarak kopyalayın

JAR dosyaları boyutları nedeniyle `.gitignore` ile repositoriden hariç tutulur.
`src/agent/` ve `src/neuralnet/` paketlerinin **hiçbir harici bağımlılığı yoktur**.
