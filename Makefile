ifeq ($(OS),Windows_NT)
	RM = del /Q /F
else
	RM = rm -f
endif

all:
	javac *.java

clean:
	$(RM) *.class